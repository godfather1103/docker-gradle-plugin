package com.github.godfather1103.gradle.entity

/**
 * <p>Title:        Godfather1103's Github</p>
 * <p>Copyright:    Copyright (c) 2023</p>
 * <p>Company:      https://github.com/godfather1103</p>
 * 授权对象
 *
 * @author  作者: Jack Chu E-mail: chuchuanbao@gmail.com<BR>
 * 创建时间：2023-07-26 23:07
 * @version 1.0
 * @since  1.0
 */
data class AuthConfig(val username: String, val password: String, val email: String = "")
