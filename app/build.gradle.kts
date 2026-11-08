    buildConfigField("String", "GITHUB_CLIENT_ID", "\"${System.getenv("GH_CLIENT_ID") ?: project.findProperty("GH_CLIENT_ID") ?: ""}\"")
