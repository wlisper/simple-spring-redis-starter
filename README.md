# 自定义spring-boot-starter

Spring-boot提供了很多应用场景的starter依赖，开发人员只需要引入starter，并进行简单的配置，就可以使用相应应用场景的功能，极大的简化了开发人员的配置，使开发人员可以专注于业务而不是繁琐的配置上。但是这些依赖也不是足够的，有时我们需要自定义starter，以提高开发效率。

## 自定义starter步骤如下

1. 理清自定义starter所需要的依赖和需要注入的功能Bean。

2. 建立starter的starter工程和autoconfiguration工程。

   参考spring-boot原生的starter组织方式，一个starter由starter工程和starter的autoconfiguration工程组成，autoconfiguration工程用于定义所有的依赖和配置，starter工程则引入autoconfiguration工程。其他工程如果依赖于这个starter，只需要引入starter工程的依赖就行了。  

3. 编写自动配置。

   这通常需要编写三个部分的配置。

   1. 配置类

      用于为容器中注入需要的配置类（Bean对象）。

      用@Configuration指明一个类为配置类；用@ConditionalOnXXX系列的注释来指明配置类的生效条件；用@Bean来为容器中注入需要的对象。

   2. 配置属性

      用于为容器中的各种Bean配置属性。

      通常使用@ConfigurationProperties注释来指明一个类是配置属性类。用@EnableConfigurationProperties来开启配置属性。

   3. 将需要启动时就加载的自动配置类，配置到resources/META-INF/spring.factories中的org.springframework.boot.autoconfigure.EnableAutoConfiguration=条目下。

## 实例：编写一个redis-starter

### 第一步：理清starter提供的服务Bean

在这个简单的redis示例starter中，我们提供一个简单的redis连接池Bean。其他的工程引入该starter，通过简单的配置redis服务地址，就可以直接使用这个redis连接池。

### 第二步：建立starter和autoconfiguration工程

新建spring-boot工程: myredis-spring-boot-starter 和myredis-spring-boot-starter-autoconfiguration。

其中myredis-spring-boot-starter依赖于myredis-spring-boot-starter-autoconfiguration, 其pom配置如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>myredis-spring-boot-starter</artifactId>
    <version>0.0.5-SNAPSHOT</version>
    <name>myredis-spring-boot-starter</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>myredis-spring-boot-starter-autoconfiguration</artifactId>
            <version>0.0.5-SNAPSHOT</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

在myredis-spring-boot-starter-autoconfiguration 工程中，引入spring-boot-starter和jedis依赖， pom如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.6.RELEASE</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>myredis-spring-boot-starter-autoconfiguration</artifactId>
    <version>0.0.5-SNAPSHOT</version>
    <name>myredis-spring-boot-starter-autoconfiguration</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
            <version>2.1.6.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>2.8.0</version>
        </dependency>
    </dependencies>
</project>
```

### 第三步：编写starter配置

1. 新建starter配置类。

   新建MyRedisAutoConfiguration类。通过@Configuration 指明它是一个配置类；通过@ConditionalOnXX配置指定配置生效的条件；通过@EnableConfigurationProperties来指明要加载的属性类（即是下一步要配置的类）。

   ```java
   @Configuration
   @ConditionalOnWebApplication
   @EnableConfigurationProperties(RedisProperties.class)
   public class MyRedisAutoConfiguration {
   
       @Autowired
       RedisProperties redisProperties;
   
       @Bean
       public JedisPool jedisPool() {
           JedisPoolConfig poolConfig = new JedisPoolConfig();
           poolConfig.setMaxTotal(redisProperties.getMaxActive());
           poolConfig.setMaxIdle(redisProperties.getMaxIdle());
           poolConfig.setMaxWaitMillis(redisProperties.getMaxWait());
           poolConfig.setTestOnBorrow(redisProperties.isTestOnBorrow());
           if (StringUtils.isEmpty(redisProperties)) {
               return new JedisPool(poolConfig, redisProperties.getHost(), redisProperties.getPort());
           } else {
               return new JedisPool(poolConfig, redisProperties.getHost(), redisProperties.getPort(),
                       Protocol.DEFAULT_TIMEOUT, redisProperties.getPassword(), Protocol.DEFAULT_DATABASE, null);
           }
       }
   }
   ```

   

2. 配置属性绑定。

   新建RedisProperties.java。用@ConfigurationProperties 指明要从配置文件中绑定属性, 这样application配置文件配置的属性字段，就会自动绑定到如下类中的同名字段。

   ```java
   @ConfigurationProperties(prefix = "myredis")
   public class RedisProperties {
   
       private String host;
       private int port;
       private String password;
       private int maxActive;
       private int maxIdle;
       private int maxWait;
   
       // getter and setters...
   }
   ```

   

3. 配置自动加载。

   在resources/META-INF/spring.factories文件中声明要自动加载我们的配置类。

   ```pro
   org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
   your.package.path.to.MyRedisAutoConfiguration
   ```



这样就完成了自定义spring boot starter的开发。通过mvn install之后，其他工程就可以直接使用了。