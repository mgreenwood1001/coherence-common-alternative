# coherence-common - An alternative

This is a fork of Oracle Coherence's coherence-common version 2.1.2.31472 which is part of The Coherence Incubator (Release 10). See  http://coherence.oracle.com/display/INC10/coherence-common for more details. This repository is not endorsed by Oracle. It is only a way for users of Coherence to experiment and extend the work done by Oracle to suggest possible evolutions of the Incubator base code.

This version upgrades the coherence.jar from 3.7.1 to 12.1.2 which caused a few classes to change signatures and dependencies on this implementation.
Added an EC2TaggedAddressProvider that allows for using IAM credentials on EC2 instances for authentication rather than using AWS keys directly.  Also
allows for the usage of tags on the instances as well as determines the state of the instance and disallows down/going down nodes from joining the cluster.


## Install

   Add coherence.jar into a local nexus repo with group-id=com.oracle, artifact-id=coherence, version=12.1.2

```
mvn install:install-file -Dfile=./coherence-12.1.2.jar -DgroupId=com.oracle \
    -DartifactId=coherence -Dversion=12.1.2 -Dpackaging=jar
```

then run the following commands:
   
```
git clone git://github.com/mgreenwood1001/coherence-common-alternative.git
cd coherence-common-alternative
mvn clean install
```
    
    
   Use the jar file inside the target directory as a dependency of your project.

## Added feature so far:

   Project was "Mavenized" although the original Ivy and additional Ant scripts are still there. 

   Added in-process Runtime from JK that can be used for testing. See http://thegridman.com/coherence/coherence-incubator-commons-runtime-package/ for more info.
   
### Address Providers

#### EC2TaggedAddressProvider

This address provider uses IAM credentials, or security keys for accessing the AWS SDK (version 1.9.16).  Specify -Dtangosol.coherence.ec2.use.iam=true
in order to use the IAM roles assigned to the node in order to access the EC2 instance tags and region attributes.

The following cloud formation configuration AWS::IAM::Role is recommended in order to allow access thru the SDK to the appropriate API commands in order
to determine instance state & tags:

```
"InstanceRole": {
      "Type": "AWS::IAM::Role",
      "Properties": {
        "AssumeRolePolicyDocument": {
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": { "Service": "ec2.amazonaws.com" },
              "Action": "sts:AssumeRole"
            }
          ]
        },
        "Path": "/",
        "Policies": [
          {
            "PolicyName": "InstancePolicy",
            "PolicyDocument": {
              "Statement": [
                {
                  "Effect": "Allow",
                  "Action": [
                    "cloudformation:DescribeStackResource",
                    "ec2:CreateTags",
                    "ec2:DescribeTags",
                    "ec2:DescribeAddresses",
                    "ec2:DescribeInstances",
                    "ec2:DescribeNetworkInterfaces",
                    "ec2:DescribeVolumes",
                  ],
                  "Resource": "*"
                }
              ]
            }
          }
        ]
      }
```

The filtering of tags as applied to EC2 instances is done via an AND operation, specify the following pairs of JVM arguments in order to filter EC2 nodes
as allow (deny all allow only those with the tags specified). 

```
-Dtangosol.coherence.ec2.tag.name#=<value>
-Dtangosol.coherence.ec2.tag.value#=<value>
```

Where # is replaced with a number.  Example:

```
-Dtangosol.coherence.ec2.tag.name1=NodeType
-Dtangosol.coherence.ec2.tag.value1=Coherence
```

The filtering of nodes that are down, or are going down happens regardless of the tag filtering rules applied.
