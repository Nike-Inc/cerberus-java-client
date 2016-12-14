# Cerberus Client

[ ![Download](https://api.bintray.com/packages/nike/maven/cerberus-client/images/download.svg) ](https://bintray.com/nike/maven/cerberus-client/_latestVersion)
[![][travis img]][travis]
[![Coverage Status](https://coveralls.io/repos/github/Nike-Inc/cerberus-java-client/badge.svg?branch=master)](https://coveralls.io/github/Nike-Inc/cerberus-java-client)
[![][license img]][license]

A java based client library for Cerberus that's built on top of Nike's Vault client.

This library acts as a wrapper around the Nike developed Vault client by configuring the client to be Cerberus compatible.

To learn more about Cerberus, please see the [Cerberus website](http://engineering.nike.com/cerberus/).

## Quickstart

### Default Client

``` java
    final VaultClient vaultClient = DefaultCerberusClientFactory.getClient();
```

#### Default URL Assumptions

The example above uses the `DefaultCerberusUrlResolver` to resolve the URL for Vault.

For that to succeed, the environment variable, `CERBERUS_ADDR`, must be set:

    CERBERUS_ADDR=https://cerberus

or the JVM system property, `cerberus.addr`, must be set:

    cerberus.addr=https://cerberus

#### Default Credentials Provider Assumptions

Again, for the example above, the `DefaultCerberusCredentialsProviderChain` is used to resolve the token needed to interact with Vault.

For that to succeed, the environment variable, `CERBERUS_TOKEN`, must be set:

    CERBERUS_TOKEN=TOKEN

or the JVM system property, `vault.token`, must be set:

    cerberus.token=TOKEN
    
or the EC2 IAM role authentication flow:

If the client library is running on an EC2 instance, it will attempt to use the instance's assigned IAM role to authenticate 
with Cerberus and obtain a token.

The IAM role must be configured for access to Cerberus before this will work.

The following policy statement must also be assigned to the IAM role, so that the client can automatically decrypt the auth token from the Cerberus IAM auth endpoint:

``` json
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Sid": "AllowKMSDecrypt",
                "Effect": "Allow",
                "Action": [
                    "kms:Decrypt"
                ],
                "Resource": [
                    "*"
                ]
            }
        ]
    }
```

### Client that can authenticate from Lambdas

#### Prerequisites

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

#### Configure the Client

``` java
    final String invokedFunctionArn = context.getInvokedFunctionArn();
    final VaultClient vaultClient = DefaultCerberusClientFactory.getClientForLambda(invokedFunctionArn);
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
