# Cerberus Client

[ ![Download](https://api.bintray.com/packages/nike/maven/cerberus-client/images/download.svg) ](https://bintray.com/nike/maven/cerberus-client/_latestVersion)
[![][travis img]][travis]
[![Coverage Status](https://coveralls.io/repos/github/Nike-Inc/cerberus-java-client/badge.svg?branch=master)](https://coveralls.io/github/Nike-Inc/cerberus-java-client)
[![][license img]][license]

A java based client library for Cerberus that's built on top of Nike's Vault client.

This library acts as a wrapper around the Nike developed Vault client by configuring the client to be Cerberus compatible.

## Quickstart

``` java
    final VaultClient vaultClient = DefaultCerberusClientFactory.getClient();
```

### Default URL Assumptions

The example above uses the `DefaultCerberusUrlResolver` to resolve the URL for Vault.

For that to succeed, the environment variable, `CERBERUS_ADDR`, must be set:

    CERBERUS_ADDR=https://cerberus

or the JVM system property, `cerberus.addr`, must be set:

    cerberus.addr=https://cerberus

### Default Credentials Provider Assumptions

Again, for the example above, the `DefaultCerberusCredentialsProviderChain` is used to resolve the token needed to interact with Vault.

For that to succeed, the environment variable, `CERBERUS_TOKEN`, must be set:

    CERBERUS_TOKEN=TOKEN

or the JVM system property, `vault.token`, must be set:

    cerberus.token=TOKEN
    
or the IAM role authentication flow:

If the client library is running on an EC2 instance, it will attempt to use the instance's assigned IAM role to authenticate 
with Cerberus and obtain a token.

The IAM role must be configured for access to Cerberus before this will work.

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
