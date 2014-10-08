# openmrs-contrib-maven-plugin
============================


A maven plugin for generating OpenMRS compatible modules. 


## omod type

This plugin provides a new customised type [omod](https://github.com/openmrs/openmrs-contrib-maven-plugin/blob/master/src/main/resources/META-INF/plexus/components.xml) type. It should be very similar to a [jar](http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html#Built-in_Lifecycle_Bindings), except for a _openmrs:initialize-module_ during _initialize_ phase, _openmrs:package-module_ during _package_ phase, and _openmrs:verify-module_ during _verify_ maven phase. 



You can check [openmrs-module-serialization.xstream](https://github.com/openmrs/openmrs-module-serialization.xstream/blob/master/omod/pom.xml#L147) as an example of a module using that type. 
To consume a module generated with omod type, make sure to add a _<type>omod</type>_ to the dependency. 



## maven plugin goals 

All the goals are non aggregator mode (i.e., will be run on each child module), and require a valid maven project. 

### openmrs:initialize-module

[InitializeModuleMojo.java](https://github.com/openmrs/openmrs-contrib-maven-plugin/blob/master/src/main/java/org/motech/openmrs/plugin/InitializeModuleMojo.java)

Binds by default to _initialize_ phase. 

```
```
