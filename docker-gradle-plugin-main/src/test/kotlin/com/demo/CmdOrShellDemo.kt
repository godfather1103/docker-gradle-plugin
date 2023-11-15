package com.demo

import com.github.godfather1103.gradle.utils.Utils
import org.junit.Test

class CmdOrShellDemo {


    @Test
    fun testShell() {
        println(Utils.checkCmdOrShellIsExist("docker version"))
        println("==============")
        println(Utils.checkCmdOrShellIsExist("dockers version"))
        println("==============")
        println(Utils.execCmdOrShell("docker version"))
        println("==============")
        println(Utils.execCmdOrShell("dockers version"))
    }
}