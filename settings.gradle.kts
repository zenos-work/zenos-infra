rootProject.name = "zenos-work"

// Subprojects mirror the repo structure
include(
    "zenos-backend",
    "zenos-frontend",
    "zenos-db",
    "zenos-infra"
)

project(":zenos-backend").projectDir  = file("../zenos-backend")
project(":zenos-frontend").projectDir = file("../zenos-frontend")
project(":zenos-db").projectDir       = file("../zenos-db")
project(":zenos-infra").projectDir    = file("../zenos-infra")