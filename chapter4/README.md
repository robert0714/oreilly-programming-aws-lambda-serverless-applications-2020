# Chapter 3

Example from Chapter 4 - this builds a Lambda artifact in a zip-file format, and also
uses the reproducible build Maven plugin.

## Prerequisites

* Created: An AWS account, and an S3 bucket for storing temporary deployment artifacts (referred to as $CF_BUCKET below)
* Installed: AWS CLI, Maven, SAM CLI
* Configured: AWS credentials in your terminal

## Usage

To build:

```
$ mvn package
```

To inspect:

```
$ zipinfo -1 target/lambda.zip
```

To deploy:

```
$ sam deploy --s3-bucket $CF_BUCKET --stack-name ChapterFour --capabilities CAPABILITY_IAM
```
or OpenTofu, the open source fork of Terraform
```bash
$ tofu fmt
$ tofu init 
$ tofu validate
$ tofu apply
```

To list the stack resources:
```
$ aws cloudformation list-stack-resources --stack-name ChapterFour
```

To test:

The above will create a new function in Lambda, so you can test via the Lambda web console,
or via the CLI using `aws lambda invoke`.
 
* Let’s get back to invocation(notice the parameter: `--cli-binary-format raw-in-base64-out`). From the terminal, run the following command:
  ```bash
  $ aws lambda invoke  \
    --invocation-type RequestResponse  \
    --function-name ChapterFour-HelloWorldLambda-KmcZ2jVSZMKV \
    --cli-binary-format raw-in-base64-out \
    --payload \"world\" outputfile.txt
  ```
  This should return the following:
  ```json
  {
    "StatusCode": 200,
    "ExecutedVersion": "$LATEST"
  }
  ```

# TEARING DOWN RESOURCES
When you run `sam deploy`, it creates or updates a CloudFormation `stack`—a set of resources that has a name, which you’ve seen already with the `--stack-name` parameter of `sam deploy`.

When you want to clean up your AWS account after trying an example, the simplest method is to find the corresponding CloudFormation stack in the AWS Web Console (in the CloudFormation section) and delete the stack using the **Delete** button.

Alternatively, you can tear down the stack from the command line. For example, to tear down the **HelloWorldLambdaJava** stack, run the following:
```bash
$ aws cloudformation delete-stack --stack-name ChapterFour 
```
or OpenTofu, the open source fork of Terraform
```bash
$ tofu destroy
```
The only example where we don’t use CloudFormation is the very first one earlier in this chapter—the HelloWorld JavaScript function—which can be deleted using the Lambda section of the AWS Web Console.

# Tests
* https://github.com/aws/serverless-java-container
* https://docs.aws.amazon.com/lambda/latest/dg/java-samples.html


# Deployment option
* Use  maven-shade-plugin
  * pom.xml
    ```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.3</version>
                <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <shadedArtifactAttached>true</shadedArtifactAttached>
                            <shadedClassifierName>aws</shadedClassifierName>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>book.HelloWorld</mainClass>
                                </transformer>
                            </transformers> 
                            <filters>
                               <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                        <exclude>META-INF/*</exclude>
                                        <exclude>META-INF/versions/**</exclude>
                                        <exclude>module-info.class</exclude>
                                    </excludes>
                                </filter>
                            </filters> 
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals> 
                    </execution>
                </executions>
            </plugin>
    ```
  * template.yaml
    ```yaml
    AWSTemplateFormatVersion: 2010-09-09
    Transform: AWS::Serverless-2016-10-31
    Description: Chapter 4

    Resources:

      HelloWorldLambda:
        Type: AWS::Serverless::Function
        Properties:
          Runtime: java17
          Handler: book.HelloWorld::handler
          CodeUri: target/programming-lambda-1.0-SNAPSHOT-aws.jar
    ```  