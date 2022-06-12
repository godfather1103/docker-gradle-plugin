# docker-gradle-plugin
> build, push the gradle plug-in of docker image.  

## Pre Description 
1. There are bugs in versions lower than or including 1.2, but they cannot be deleted after uploading, so it is recommended to use the version >= 1.2.1

## Code Repository
GitHub: [https://github.com/godfather1103/docker-gradle-plugin](https://github.com/godfather1103/docker-gradle-plugin)  
Gitee: [https://gitee.com/godfather1103/docker-gradle-plugin](https://gitee.com/godfather1103/docker-gradle-plugin)

## Usage
1. Introducing relevant plug-ins into gradle
```groovy
// groovy DSL
// Using the plugins DSL:
plugins {
  id "io.github.godfather1103.docker-plugin" version "1.2.3"
}

// Using legacy plugin application:
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "io.github.godfather1103:docker-plugin:1.2.3"
    }
}
apply plugin: "io.github.godfather1103.docker-plugin"

// kotlin DSL
// Using the plugins DSL:
plugins {
    id("io.github.godfather1103.docker-plugin") version "1.2.3"
}

// Using legacy plugin application:
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.godfather1103:docker-plugin:1.2.3")
    }
}
apply(plugin = "io.github.godfather1103.docker-plugin")
```

2. Configure relevant build parameters
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

## Tasks
> Plugin introduces the following tasks.

| Task                   | Description          |
|:-----------------------|:---------------------|
| <kbd>dockerBuild</kbd> | build  docker image. |


## Setup DSL
> The following attributes are a part of the Setup DSL <kbd>docker { ... }</kbd> in which allows you to set up the environment and dependencies.  

| Attributes                                                          | Values                                                                                                                                            |
|:--------------------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------|
| <kbd>imageName</kbd> - Build image name                             | **Acceptable Values:** <br/><kbd>String</kbd> - `'demoImage'` <br/><br/>**Default Value:** <kbd>''</kbd>                                          |
| <kbd>dockerDirectory</kbd> - Directory corresponding to docker      | **Acceptable Values:** <br/><kbd>Path</kbd> - `'${project.projectDir}/'` <br/><br/>**Default Value:** <kbd>''</kbd>                               |
| <kbd>dockerBuildDependsOn</kbd> - Task of building image dependency | **Acceptable Values:** <br/><kbd>TaskName</kbd> - `'bootJar'` <br/><br/>**Default Value:** <kbd>''</kbd>                                          |
| <kbd>dockerImageTags</kbd> - Built tag list                         | **Acceptable Values:** <br/><kbd>String</kbd> - `'1.0'` <br/><br/>**Default Value:** <kbd>''</kbd>                                                |
| <kbd>pushImage</kbd> - Whether to push the corresponding image      | **Acceptable Values:** <br/><kbd>Boolean</kbd> - `true` <br/><br/>**Default Value:** <kbd>false</kbd>                                             |
| <kbd>pushImageTag</kbd> - Push tag?                                 | **Acceptable Values:** <br/><kbd>Boolean</kbd> - `true` <br/><br/>**Default Value:** <kbd>false</kbd>                                             |
| <kbd>auth</kbd> - Authentication information                        | **Acceptable Values:** <br/><kbd>AuthConfig Object</kbd> - `new AuthConfig(username,password,email)` <br/><br/>**Default Value:** <kbd>null</kbd> |
| <kbd>dockerBuildArgs</kbd> - docker build args                      | **Acceptable Values:** <br/><kbd>Map</kbd> - `map["arg1"]="arg"` <br/><br/>**Default Value:** <kbd>null</kbd>                                     |
| <kbd>resources</kbd> - Resources copied during the build process    | **Acceptable Values:** <br/><kbd>List[Resource]</kbd> - `[resources1,resources2,...]` <br/><br/>**Default Value:** <kbd>[]</kbd>                  |

### AuthConfig Properties

| Attributes                                       | Values                                                                                                             |
|:-------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------|
| <kbd>username</kbd> - UserName of docker account | **Acceptable Values:** <br/><kbd>String</kbd> - `'username'` <br/><br/>**Default Value:** <kbd>''</kbd>            |
| <kbd>password</kbd> - Password of docker account | **Acceptable Values:** <br/><kbd>String</kbd> - `'password'` <br/><br/>**Default Value:** <kbd>''</kbd>            |
| <kbd>email</kbd> - Email of docker account       | **Acceptable Values:** <br/><kbd>String</kbd> - `'example@example.com'` <br/><br/>**Default Value:** <kbd>''</kbd> |

### Resource Properties

| Attributes                                            | Values                                                                                                                 |
|:------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------|
| <kbd>directory</kbd> - From path                      | **Acceptable Values:** <br/><kbd>String</kbd> - `'/'` <br/><br/>**Default Value:** <kbd>''</kbd>                       |
| <kbd>targetPath</kbd> - Target path                   | **Acceptable Values:** <br/><kbd>String</kbd> - `'build/docker'` <br/><br/>**Default Value:** <kbd>''</kbd>            |
| <kbd>includes</kbd> - Introduced resources(Ant-style) | **Acceptable Values:** <br/><kbd>List[String]</kbd> - `['*.jar','*.class']` <br/><br/>**Default Value:** <kbd>[]</kbd> |
| <kbd>excludes</kbd> - Excluded resources(Ant-style)   | **Acceptable Values:** <br/><kbd>List[String]</kbd> - `['*.log','log/**']` <br/><br/>**Default Value:** <kbd>[]</kbd>  |


### Configure default docker account information
> Copy the following to your gradle.properties file.When the corresponding authentication information is not configured in the project, the default account information will be used.

| Attributes                                              | Values                                                                  |
|:--------------------------------------------------------|:------------------------------------------------------------------------|
| <kbd>docker.username</kbd> - UserName of docker account | **Acceptable Values:** <br/><kbd>String</kbd> - `'username'`            |
| <kbd>docker.password</kbd> - Password of docker account | **Acceptable Values:** <br/><kbd>String</kbd> - `'password'`            |
| <kbd>docker.email</kbd> - Email of docker account       | **Acceptable Values:** <br/><kbd>String</kbd> - `'example@example.com'` |

## Donate
Your gift will help me to contribute better, thank you!

[PayPal](https://paypal.me/godfather1103?locale.x=zh_XC)

Alipay  
<img src="https://s4.ax1x.com/2021/12/16/T9B04f.png" alt="支付宝" width="200" height="300" align="bottom" />   
<img src="https://s4.ax1x.com/2021/12/16/T9BI8U.png" alt="支付宝" width="200" height="300" align="bottom" />   
WeChatPay  
<img src="https://s.pc.qq.com/tousu/img/20200815/9185636_1597474776.jpg" alt="微信支付" width="300" height="320" align="bottom" />


## Epilogue
idea from [docker-maven-plugin](https://github.com/spotify/docker-maven-plugin)

