plugins {
    id("ecbuild.java-conventions")
    id("ecbuild.copy-conventions")
}

extra.set("copyTo", "{server}/plugins")

dependencies {
    compileOnly(project(":nukkit"))
    compileOnly(project(":ECCommons"))
}

group = "me.onebone"
description = "Actaeon"
