# docker-gradle-plugin
> 构建，推送docker镜像的Gradle插件  

### English Readme [click here](README.EN.md)

## 用法
1. 在build.gradle中引入相关插件
```groovy
// groovy DSL
// Using the plugins DSL:
plugins {
  id "io.github.godfather1103.docker-plugin" version "1.1"
}

// Using legacy plugin application:
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "io.github.godfather1103:docker-plugin:1.1"
    }
}
apply plugin: "io.github.godfather1103.docker-plugin"

// kotlin DSL
// Using the plugins DSL:
plugins {
    id("io.github.godfather1103.docker-plugin") version "1.1"
}

// Using legacy plugin application:
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.godfather1103:docker-plugin:1.1")
    }
}
apply(plugin = "io.github.godfather1103.docker-plugin")
```

2. 配置相关构建参数
```groovy
// groovy DSL
docker {
    dockerBuildDependsOn.add("bootJar")
    dockerDirectory.value(project.projectDir.absolutePath)
    def user = (project.findProperty("docker.username") ?: "").toString()
    def password = (project.findProperty("docker.password") ?: "").toString()
    def email = (project.findProperty("docker.email") ?: "").toString()
    def name = (project.findProperty("docker.demo.imageName") ?: "").toString()
    if (!user.isEmpty() && !password.isEmpty()) {
        auth.value(new AuthConfig(user, password, email))
    }
    imageName.value(name + "-groovy")
    dockerImageTags.add("1.0")
    pushImageTag.value(true)
    pushImage.value(true)
}
// kotlin DSL
docker {
    dockerBuildDependsOn.add("bootJar")
    dockerDirectory.value(project.projectDir.absolutePath)
    val user = (project.findProperty("docker.username") ?: "") as String
    val password = (project.findProperty("docker.password") ?: "") as String
    val email = (project.findProperty("docker.email") ?: "") as String
    val name = (project.findProperty("docker.demo.imageName") ?: "demo") as String
    if (user.isNotEmpty() && password.isNotEmpty()) {
        auth.value(AuthConfig(user, password, email))
    }
    imageName.value("$name-kotlin")
    dockerImageTags.add("1.0")
    pushImageTag.value(true)
    pushImage.value(true)
}
```

## 配置DSL
> 以下属性是设置DSL docker{…}的一部分其中允许您设置环境和依赖项。  

| 配置项                                         | 属性值                                                                                                     |
|:--------------------------------------------|:--------------------------------------------------------------------------------------------------------|
| <kbd>imageName</kbd> - 构建的镜像名               | **允许的值:** <br/><kbd>字符串</kbd> - `'demoImage'` <br/><br/>**默认值:** <kbd>''</kbd>                          |
| <kbd>dockerDirectory</kbd> - docker对应的目录    | **允许的值:** <br/><kbd>路径</kbd> - `'${project.projectDir}/'` <br/><br/>**默认值:** <kbd>''</kbd>              |
| <kbd>dockerBuildDependsOn</kbd> - 构建镜像依赖的任务 | **允许的值:** <br/><kbd>任务名</kbd> - `'bootJar'` <br/><br/>**默认值:** <kbd>''</kbd>                            |
| <kbd>dockerImageTags</kbd> - 构建的tag列表       | **允许的值:** <br/><kbd>字符串</kbd> - `'1.0'` <br/><br/>**默认值:** <kbd>''</kbd>                                |
| <kbd>pushImage</kbd> - 是否推送对应的镜像            | **允许的值:** <br/><kbd>布尔值</kbd> - `true` <br/><br/>**默认值:** <kbd>false</kbd>                              |
| <kbd>pushImageTag</kbd> - 是否推送tag           | **允许的值:** <br/><kbd>布尔值</kbd> - `true` <br/><br/>**默认值:** <kbd>false</kbd>                              |
| <kbd>auth</kbd> - 认证信息                      | **允许的值:** <br/><kbd>AuthConfig对象</kbd> - `new AuthConfig(用户名,密码,邮箱)` <br/><br/>**默认值:** <kbd>null</kbd> |

### AuthConfig对象属性值

| 配置项                                | 属性值                                                                                      |
|:-----------------------------------|:-----------------------------------------------------------------------------------------|
| <kbd>username</kbd> - docker账户的用户名 | **允许的值:** <br/><kbd>字符串</kbd> - `'username'` <br/><br/>**默认值:** <kbd>''</kbd>            |
| <kbd>password</kbd> - docker账户的密码  | **允许的值:** <br/><kbd>字符串</kbd> - `'password'` <br/><br/>**默认值:** <kbd>''</kbd>            |
| <kbd>email</kbd> - docker账户的email  | **允许的值:** <br/><kbd>字符串</kbd> - `'example@example.com'` <br/><br/>**默认值:** <kbd>''</kbd> |


## 写在最后
该插件相关创意来源于[docker-maven-plugin](https://github.com/spotify/docker-maven-plugin)


