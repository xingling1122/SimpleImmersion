# SimpleImmersion  
Better use of Android Immersion  
How to use SimpleImmersion  

gradle:  

Step 1. Add repository to your build file  

Add it in your root build.gradle at the end of repositories:  
``
allprojects {  
	repositories {  
		...  
		maven { url 'https://jitpack.io' }  
	}  
}  
``
Step 2. Add the dependency  
``
dependencies {  
        implementation 'com.github.xingling1122:SimpleImmersion:1.0'  
}  
``
maven:  

Step 1. Add repository to your build file  

Add it in your root build.gradle at the end of repositories:  
``
<repositories>  
	<repository>  
	    <id>jitpack.io</id>  
	    <url>https://jitpack.io</url>  
	</repository>  
</repositories>  
``
Step 2. Add the dependency  
``
<dependency>  
    <groupId>com.github.xingling1122</groupId>  
    <artifactId>SimpleImmersion</artifactId>  
    <version>1.0</version>  
</dependency>  
``
