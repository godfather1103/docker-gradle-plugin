# English Readme [click here](README.EN.md)

# docker-gradle-plugin
> 构建，推送docker镜像的Gradle插件  

## 代码库
GitHub: [https://github.com/godfather1103/docker-gradle-plugin](https://github.com/godfather1103/docker-gradle-plugin)  
Gitee/码云: [https://gitee.com/godfather1103/docker-gradle-plugin](https://gitee.com/godfather1103/docker-gradle-plugin)  

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

## 任务
> 插件包含以下任务。

| 任务                     | 描述          |
|:-----------------------|:------------|
| <kbd>dockerBuild</kbd> | 构建docker镜像。 |


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
| <kbd>dockerBuildArgs</kbd> - docker build参数 | **允许的值:** <br/><kbd>Map</kbd> - `map["arg1"]="arg"` <br/><br/>**默认值:** <kbd>null</kbd>                  |
| <kbd>resources</kbd> - 构建过程中复制的资源           | **允许的值:** <br/><kbd>List[Resource]</kbd> - `[Resource1,...,ResourceN]` <br/><br/>**默认值:** <kbd>[]</kbd> |

### AuthConfig对象属性值

| 配置项                                | 属性值                                                                                      |
|:-----------------------------------|:-----------------------------------------------------------------------------------------|
| <kbd>username</kbd> - docker账户的用户名 | **允许的值:** <br/><kbd>字符串</kbd> - `'username'` <br/><br/>**默认值:** <kbd>''</kbd>            |
| <kbd>password</kbd> - docker账户的密码  | **允许的值:** <br/><kbd>字符串</kbd> - `'password'` <br/><br/>**默认值:** <kbd>''</kbd>            |
| <kbd>email</kbd> - docker账户的email  | **允许的值:** <br/><kbd>字符串</kbd> - `'example@example.com'` <br/><br/>**默认值:** <kbd>''</kbd> |

### 配置默认的docker账户信息
> 用户可以在gradle.properties中配置以下参数作为默认的账户信息，当项目中未配置对应的认证信息信息时，将使用默认的账户信息。  

| 配置项                                       | 属性值                                                     |
|:------------------------------------------|:--------------------------------------------------------|
| <kbd>docker.username</kbd> - docker账户的用户名 | **允许的值:** <br/><kbd>字符串</kbd> - `'username'`            |
| <kbd>docker.password</kbd> - docker账户的密码  | **允许的值:** <br/><kbd>字符串</kbd> - `'password'`            |
| <kbd>docker.email</kbd> - docker账户的email  | **允许的值:** <br/><kbd>字符串</kbd> - `'example@example.com'` |

## 捐赠
你的馈赠将助力我更好的去贡献，谢谢！  

[PayPal](https://paypal.me/godfather1103?locale.x=zh_XC)

支付宝  
<img src="https://s4.ax1x.com/2021/12/16/T9B04f.png" alt="支付宝" width="200" height="300" align="bottom" />   
<img src="https://s4.ax1x.com/2021/12/16/T9BI8U.png" alt="支付宝" width="200" height="300" align="bottom" />   
微信  
<img src="https://s.pc.qq.com/tousu/img/20200815/9185636_1597474776.jpg" alt="微信支付" width="300" height="320" align="bottom" />


## 写在最后
该插件相关创意来源于[docker-maven-plugin](https://github.com/spotify/docker-maven-plugin)


