# Cerberus Client

[ ![Download](https://api.bintray.com/packages/nike/maven/cerberus-client/images/download.svg) ](https://bintray.com/nike/maven/cerberus-client/_latestVersion)
[![][travis img]][travis]
[![Coverage Status](https://coveralls.io/repos/github/Nike-Inc/cerberus-java-client/badge.svg?branch=master)](https://coveralls.io/github/Nike-Inc/cerberus-java-client)
[![][license img]][license]

A java based client library for Cerberus that's built on top of Nike's Vault client.

This library acts as a wrapper around the Nike developed Vault client by configuring the client to be Cerberus compatible.

To learn more about Cerberus, please see the [Cerberus website](http://engineering.nike.com/cerberus/).

## Quickstart for EC2

1. Start with the [quick start guide](http://engineering.nike.com/cerberus/docs/user-guide/quick-start).
2. Add the [Cerberus client dependency](https://bintray.com/nike/maven/cerberus-client) to your build (e.g. Maven, Gradle)
3. Provide an authentication mechanism.
   - For local development it is easiest to export a `CERBERUS_TOKEN` that you copied from the Cerberus dashboard.
     When running in AWS, your application will not need this environmetal variable, instead it will automatically 
     authenticate using its IAM role.
   - If you would like to test IAM authentication locally, you can do that by [assuming a role](http://docs.aws.amazon.com/cli/latest/userguide/cli-roles.html).
4. Access secrets from Cerberus using Java
``` java
    String cerberusUrl = "https://cerberus.example.com";
    VaultClient vaultClient = DefaultCerberusClientFactory.getClient(cerberusUrl);
    Map<String,String> secrets = vaultClient.read("/app/my-sdb-name").getData();
```

## Lambdas

Generally it does NOT make sense to store Lambda secrets in Cerberus for two reasons:

1. Cerberus cannot support the scale that lambdas may need, e.g. thousands of requests per second
1. Lambdas will not want the extra latency needed to authenticate and read from Cerberus

A better solution for Lambda secrets is using the [encrypted environmental variables](http://docs.aws.amazon.com/lambda/latest/dg/env_variables.html)
feature provided by AWS.

Another option is to store Lambda secrets in Cerberus but only read them at Lambda deploy time, then storing them as encrypted
environmental variables, to avoid the extra Cerberus runtime latency.

### Additional permissions

The IAM role assigned to the Lambda function must contain the following policy statement in addition to the above KMS decrypt policy, this is so the Lambda can look up its metadata to automatically authenticate with the Cerberus IAM auth endpoint:

``` json
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "AllowGetFunctionConfig",
                "Effect": "Allow",
                "Action": [
                    "lambda:GetFunctionConfiguration"
                ],
                "Resource": [
                    "*"
                ]
            }
        ]
    }
```

### Configure the Client

Setup the CERBERUS_ADDR environmental variable and access Cerberus using Java:

``` java
    String invokedFunctionArn = context.getInvokedFunctionArn();
    VaultClient vaultClient = DefaultCerberusClientFactory.getClientForLambda(invokedFunctionArn);
    Map<String,String> secrets = vaultClient.read("/app/my-sdb-name").getData();
```

## More Configuration Options

There are other ways of using this library than the quick start above.

### Configuring the Cerberus URL

Provide the URL directly using the factory method `DefaultCerberusClientFactory.getClient(cerberusUrl)` or use the
`DefaultCerberusUrlResolver` by setting the environment variable `CERBERUS_ADDR` or the JVM system property `cerberus.addr`
and then use the factory method that does not require a URL:

``` java
    final VaultClient vaultClient = DefaultCerberusClientFactory.getClient();
    Map<String,String> secrets = vaultClient.read("/app/my-sdb-name").getData();
```

### Configuring Credentials

#### Default Credentials Provider Chain

This client uses a provider chain to resolve the token needed to interact with Cerberus.

See `DefaultCerberusCredentialsProviderChain.java` for full usage.

If the client library is running on an EC2 instance, it will attempt to use the instance's assigned IAM role to authenticate 
with Cerberus and obtain a token.

The IAM role must be configured for access to Cerberus before this will work.

The following policy statement must also be assigned to the IAM role, so that the client can automatically decrypt the auth token from the Cerberus IAM auth endpoint:

``` json
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "AllowKmsDecrypt",
                "Effect": "Allow",
                "Action": [
                    "kms:Decrypt"
                ],
                "Resource": [
                    "arn:aws:kms:*:[Cerberus AWS Account ID]:key/*"
                ]
            }
        ]
    }
```

The account ID in the ARN should be the account ID where Cerberus is deployed.  See your company's internal
documentation for the account ID that you should use.

## Development

### Run Integration Tests

First, make sure the following environment variables are set before running the Java Client integration tests:

``` bash
    export CERBERUS_ADDR=https://example.cerberus.com
    export TEST_ACCOUNT_ID=12345678910
    export TEST_ROLE_NAME=integration-test-role
    export TEST_REGION=us-west-2
```

Next, in the project directory run:
```gradle
    ./gradlew integration
```

## Further Details

Cerberus client is a small project. It only has a few classes and they are all fully documented. For further details please see the source code, including javadocs and unit tests.

<a name="license"></a>
## License

Cerberus client is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[travis]:https://travis-ci.org/Nike-Inc/cerberus-java-client
[travis img]:https://api.travis-ci.org/Nike-Inc/cerberus-java-client.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[toc]:#table_of_contents
