# Cerberus Client

[![codecov](https://codecov.io/gh/Nike-Inc/cerberus-java-client/branch/master/graph/badge.svg)](https://codecov.io/gh/Nike-Inc/cerberus-java-client)
[![][license img]][license]

This is a Java based client library for Cerberus that is built on top of Nike's Cerberus client.

This library acts as a wrapper around the Nike developed Cerberus client by configuring the client to be Cerberus compatible.

To learn more about Cerberus, please see the [Cerberus website](http://engineering.nike.com/cerberus/).
 
## <span style="color:#d9534f">Publishing Notice 3/17/2021</span>
As of spring 2021, JFrog has decided to sunset Bintray and JCenter.
Due to this decision, we are pausing our open source publishing of the Cerberus Client indefinitely.
However, we will still be updating the source code and making new GitHub releases.

In order to build the jar yourself, run this command:
```bash
./gradlew assemble
```

The jar will be located in `./build/libs/`.

## Quickstart for Cerberus Java Client

1. Add the [Cerberus client dependency](https://bintray.com/nike/maven/cerberus-client) to your build (e.g. Maven, Gradle).
1. Access secrets from Cerberus using Java.
``` java
    String cerberusUrl = "https://cerberus.example.com";
    String region = "us-west-2";
    CerberusClient cerberusClient = DefaultCerberusClientFactory.getClient(cerberusUrl, region);
    Map<String,String> secrets = cerberusClient.read("/app/my-sdb-name").getData();
```
Check out ["Working with AWS Credentials"](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html) for more information on how the AWS SDK for Java loads credentials.


## Manage Safe Deposit Box

### Create Safe Deposit Box
Your IAM role or user needs to be added to any safe deposit box to be authorized to create a safe deposit box.
``` java
    String cerberusUrl = "https://cerberus.example.com";
    String region = "us-west-2";
    CerberusClient cerberusClient = DefaultCerberusClientFactory.getClient(cerberusUrl, region);
    String appCategoryId = cerberusClient.getCategoryIdByPath("app");
    Map<CerberusRolePermission, String> rolePermissionMap = cerberusClient.getRolePermissionMap();
    CerberusSafeDepositBoxResponse newSdb = cerberusClient.createSafeDepositBox(CerberusSafeDepositBoxRequest.newBuilder()
                    .withName("cerberus secrets")
                    .withOwner("very important user group")
                    .withCategoryId(appCategoryId)
                    .withRolePermissionMap(rolePermissionMap)
                    .withIamPrincipalPermission("arn:aws:iam::12345:role/ec2-role", OWNER)
                    .withUserGroupPermission("readonly group", READ)
                    .build());
```

### Update Safe Deposit Box
Your IAM role or user needs to be the `owner` of a safe deposit box to update it.
``` java
    String cerberusUrl = "https://cerberus.example.com";
    String region = "us-west-2";
    CerberusClient cerberusClient = DefaultCerberusClientFactory.getClient(cerberusUrl, region);
    Map<CerberusRolePermission, String> rolePermissionMap = cerberusClient.getRolePermissionMap();
    CerberusSafeDepositBoxResponse sdb = cerberusClient.getSafeDepositBoxByName("cerberus secrets");
    cerberusClient.updateSafeDepositBox(CerberusSafeDepositBoxRequest.newBuilder()
                    .withCerberusSafeDepositBoxResponse(sdb)
                    .withRolePermissionMap(rolePermissionMap)
                    .withIamPrincipalPermission("arn:aws:iam::12345:role/lambda-role", READ)
                    .build());
```

### Delete Safe Deposit Box
Your IAM role or user needs to be the `owner` of a safe deposit box to delete it.
``` java
    String cerberusUrl = "https://cerberus.example.com";
    String region = "us-west-2";
    CerberusClient cerberusClient = DefaultCerberusClientFactory.getClient(cerberusUrl, region);
    Map<CerberusRolePermission, String> rolePermissionMap = cerberusClient.getRolePermissionMap();
    CerberusSafeDepositBoxResponse sdb = cerberusClient.getSafeDepositBoxByName("cerberus secrets");
    cerberusClient.deleteSafeDepositBox(sdb.getId());
```


## Development

### Run Integration Tests

First, make sure the following environment variables are set before running the Java Client integration tests:

``` bash
    export CERBERUS_ADDR=https://example.cerberus.com
    export TEST_REGION=us-west-2
```

Then, make sure AWS credentials have been obtained. One method is by running [gimme-aws-creds](https://github.com/Nike-Inc/gimme-aws-creds):

```bash
    gimme-aws-creds
```

Next, in the project directory run:
```gradle
    ./gradlew integration
```

<a name="license"></a>
## License

Cerberus client is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

[travis]:https://travis-ci.org/Nike-Inc/cerberus-java-client
[travis img]:https://api.travis-ci.org/Nike-Inc/cerberus-java-client.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[toc]:#table_of_contents
