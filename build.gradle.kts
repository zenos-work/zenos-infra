// ─────────────────────────────────────────────────────────────────────────────
// zenos.work — Root Gradle Build
//
// Why Gradle?
//   Wrangler is the current deploy tool. Wrapping it in Gradle tasks means:
//   - One build system for all repos (frontend, backend, db, infra)
//   - Swap Wrangler for any other tool later by changing ONE task body
//   - Standard lifecycle: build → test → lint → deploy
//   - CI/CD pipelines call `./gradlew deploy` regardless of underlying tool
//
// Current tool binding:  Wrangler (Cloudflare)
// Future tool options:   Any ASGI host (Railway, Fly.io, AWS Lambda, etc.)
//                        Replace `wrangler(...)` calls with the new CLI
// ─────────────────────────────────────────────────────────────────────────────

// ── Gradle plugin for running shell commands ──────────────────────────────────
plugins {
    base
}

// ── Build properties ──────────────────────────────────────────────────────────
val backendDir  = file("../zenos-backend")
val frontendDir = file("../zenos-frontend")
val dbDir       = file("../zenos-db")
val env         = project.findProperty("env")?.toString() ?: "production"
val isStaging   = env == "staging"
val wranglerEnv = if (isStaging) "--env staging" else "--env \"\""

// ── Utility: run a shell command and stream output ───────────────────────────
fun Project.shell(command: String, workingDir: File = rootDir) {
    exec {
        commandLine("bash", "-c", command)
        this.workingDir = workingDir
        standardOutput = System.out
        errorOutput    = System.err
    }
}

// ── Utility: run wrangler — SWAP THIS FUNCTION to migrate away from Wrangler ─
// Replace the body of this function with your new deploy tool CLI call.
// The rest of the build file stays unchanged.
fun Project.wrangler(command: String, workingDir: File = backendDir) {
    shell("wrangler $command $wranglerEnv", workingDir)
}

// ─────────────────────────────────────────────────────────────────────────────
// BACKEND TASKS
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("backend:lint") {
    group = "backend"
    description = "Run ruff linter on Python source"
    doLast {
        shell("source .venv/bin/activate && ruff check src/", backendDir)
    }
}

tasks.register("backend:format") {
    group = "backend"
    description = "Run ruff formatter on Python source"
    doLast {
        shell("source .venv/bin/activate && ruff format src/", backendDir)
    }
}

tasks.register("backend:test") {
    group = "backend"
    description = "Run Python unit tests"
    doLast {
        shell("source .venv/bin/activate && pytest tests/ -v", backendDir)
    }
}

tasks.register("backend:dev") {
    group = "backend"
    description = "Start local dev server on localhost:8787"
    doLast {
        shell("source .venv/bin/activate && wrangler dev --env \"\"", backendDir)
    }
}

tasks.register("backend:build") {
    group = "backend"
    description = "Validate + lint before deploy (no artifact — Workers deploy from source)"
    dependsOn("backend:lint")
    doLast {
        println("✓ Backend build validated for env=$env")
    }
}

tasks.register("backend:deploy") {
    group = "backend"
    description = "Deploy Python Worker to Cloudflare (env=$env)"
    dependsOn("backend:build")
    doLast {
        // ── TO MIGRATE AWAY FROM WRANGLER: replace this wrangler() call ──────
        // Example: shell("flyctl deploy --remote-only", backendDir)
        // Example: shell("railway up", backendDir)
        wrangler("deploy")
        println("✓ Backend deployed to $env")
    }
}

tasks.register("backend:tail") {
    group = "backend"
    description = "Stream live logs from deployed Worker"
    doLast {
        wrangler("tail")
    }
}

tasks.register("backend:secret:put") {
    group = "backend"
    description = "Set a wrangler secret. Usage: ./gradlew backend:secret:put -Pname=JWT_SECRET"
    doLast {
        val secretName = project.findProperty("name")
            ?: error("Pass secret name with -Pname=SECRET_NAME")
        wrangler("secret put $secretName")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FRONTEND TASKS
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("frontend:install") {
    group = "frontend"
    description = "Install npm dependencies"
    doLast {
        shell("pnpm install", frontendDir)
    }
}

tasks.register("frontend:lint") {
    group = "frontend"
    description = "Run ESLint on TypeScript source"
    doLast {
        shell("pnpm run lint", frontendDir)
    }
}

tasks.register("frontend:test") {
    group = "frontend"
    description = "Run frontend unit tests"
    doLast {
        shell("pnpm run test", frontendDir)
    }
}

tasks.register("frontend:dev") {
    group = "frontend"
    description = "Start Vite dev server on localhost:5173"
    doLast {
        shell("pnpm run dev", frontendDir)
    }
}

tasks.register("frontend:build") {
    group = "frontend"
    description = "Build React app for production"
    dependsOn("frontend:lint")
    doLast {
        shell("pnpm run build", frontendDir)
        println("✓ Frontend built")
    }
}

tasks.register("frontend:deploy") {
    group = "frontend"
    description = "Deploy frontend to Cloudflare Pages (env=$env)"
    dependsOn("frontend:build")
    doLast {
        // ── TO MIGRATE AWAY FROM CLOUDFLARE PAGES: replace this block ────────
        // Example: shell("netlify deploy --prod --dir=dist", frontendDir)
        // Example: shell("vercel --prod", frontendDir)
        val project = if (isStaging) "zenos-frontend-staging" else "zenos-frontend"
        shell(
            "wrangler pages deploy dist --project-name=$project",
            frontendDir
        )
        println("✓ Frontend deployed to $env")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DATABASE TASKS
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("db:migrate:local") {
    group = "database"
    description = "Run all pending migrations against local D1"
    doLast {
        // ── TO MIGRATE AWAY FROM D1: replace with your DB migration tool ─────
        // Example: shell("flyway migrate -url=jdbc:sqlite:./dev.db", dbDir)
        // Example: shell("alembic upgrade head", dbDir)
        shell("./scripts/migrate.sh local", dbDir)
        println("✓ Local migrations applied")
    }
}

tasks.register("db:migrate:staging") {
    group = "database"
    description = "Run all pending migrations against staging D1"
    doLast {
        shell("./scripts/migrate.sh staging", dbDir)
        println("✓ Staging migrations applied")
    }
}

tasks.register("db:migrate:production") {
    group = "database"
    description = "Run all pending migrations against production D1"
    doLast {
        println("⚠  Running PRODUCTION migrations. Ctrl+C within 5s to cancel.")
        Thread.sleep(5000)
        shell("./scripts/migrate.sh prd", dbDir)
        println("✓ Production migrations applied")
    }
}

tasks.register("db:query") {
    group = "database"
    description = "Run ad-hoc SQL. Usage: ./gradlew db:query -Psql='SELECT ...' -Penv=local"
    doLast {
        val sql    = project.findProperty("sql") ?: error("Pass SQL with -Psql='SELECT ...'")
        val target = project.findProperty("env")?.toString() ?: "local"
        val remote = if (target == "local") "--local" else "--remote"
        val db     = if (target == "staging") "staging-zenos-blog-db" else "prd-zenos-blog-db"
        shell(
            "wrangler d1 execute $db $remote " +
            "--config ../zenos-backend/wrangler.jsonc " +
            "--command \"$sql\"",
            dbDir
        )
    }
}

tasks.register("db:shell") {
    group = "database"
    description = "Run a SQL file. Usage: ./gradlew db:shell -Pfile=/tmp/q.sql -Penv=local"
    doLast {
        val file   = project.findProperty("file") ?: error("Pass -Pfile=/path/to/file.sql")
        val target = project.findProperty("env")?.toString() ?: "local"
        val remote = if (target == "local") "--local" else "--remote"
        val db     = if (target == "staging") "staging-zenos-blog-db" else "prd-zenos-blog-db"
        shell(
            "wrangler d1 execute $db $remote " +
            "--config ../zenos-backend/wrangler.jsonc " +
            "--file $file",
            dbDir
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP-LEVEL LIFECYCLE TASKS
// ─────────────────────────────────────────────────────────────────────────────

tasks.register("lint") {
    group = "verification"
    description = "Lint all projects"
    dependsOn("backend:lint", "frontend:lint")
}

tasks.register("test") {
    group = "verification"
    description = "Test all projects"
    dependsOn("backend:test", "frontend:test")
}

tasks.register("build") {
    group = "build"
    description = "Build all projects"
    dependsOn("backend:build", "frontend:build")
}

tasks.register("deploy") {
    group = "deployment"
    description = "Deploy everything to env=$env  (./gradlew deploy -Penv=staging)"
    dependsOn("backend:deploy", "frontend:deploy")
    doLast {
        println("✓ Full deploy complete → env=$env")
    }
}

tasks.register("dev") {
    group = "development"
    description = "Print instructions for starting local dev environment"
    doLast {
        println("""
            Start local dev environment:
              Terminal 1:  ./gradlew backend:dev
              Terminal 2:  ./gradlew frontend:dev
              Terminal 3:  ./gradlew db:migrate:local

            Backend:   http://localhost:8787
            Frontend:  http://localhost:5173
        """.trimIndent())
    }
}

tasks.register("help:zenos") {
    group = "help"
    description = "Show all available zenos.work build tasks"
    doLast {
        println("""
            ╔══════════════════════════════════════════════════════════════╗
            ║              zenos.work — Gradle Build Tasks                 ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  LIFECYCLE                                                   ║
            ║    ./gradlew lint                    lint all projects       ║
            ║    ./gradlew test                    test all projects       ║
            ║    ./gradlew build                   build all projects      ║
            ║    ./gradlew deploy                  deploy to production    ║
            ║    ./gradlew deploy -Penv=staging     deploy to staging      ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  BACKEND                                                     ║
            ║    ./gradlew backend:dev             localhost:8787          ║
            ║    ./gradlew backend:lint            ruff check              ║
            ║    ./gradlew backend:test            pytest                  ║
            ║    ./gradlew backend:deploy          deploy Worker           ║
            ║    ./gradlew backend:tail            stream logs             ║
            ║    ./gradlew backend:secret:put      set a secret            ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  FRONTEND                                                    ║
            ║    ./gradlew frontend:dev            localhost:5173          ║
            ║    ./gradlew frontend:lint           eslint                  ║
            ║    ./gradlew frontend:test           vitest                  ║
            ║    ./gradlew frontend:build          vite build              ║
            ║    ./gradlew frontend:deploy         deploy Pages            ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  DATABASE                                                    ║
            ║    ./gradlew db:migrate:local        local D1                ║
            ║    ./gradlew db:migrate:staging      staging D1              ║
            ║    ./gradlew db:migrate:production   prod D1 (5s warning)    ║
            ║    ./gradlew db:query -Psql='...'    ad-hoc query            ║
            ║    ./gradlew db:shell -Pfile=f.sql   run SQL file            ║
            ╠══════════════════════════════════════════════════════════════╣
            ║  TO MIGRATE AWAY FROM WRANGLER                               ║
            ║    1. Open build.gradle.kts                                  ║
            ║    2. Replace the wrangler() function body with new CLI      ║
            ║    3. Update frontend:deploy shell() call                    ║
            ║    4. Update db:migrate:* shell() calls                      ║
            ║    All ./gradlew commands stay identical for the team        ║
            ╚══════════════════════════════════════════════════════════════╝
        """.trimIndent())
    }
}