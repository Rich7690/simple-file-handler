## Overview

This project contains the source code for a simple web app that handles uploads and downloads of files.

## Assumptions
- No handling security or anything of that nature (No SSL, auth etc.)
- Scaling/design given a four hour time frame (no exhausting a production level application design).
- Requirements open to some interpretation (see requirements chosen and design section farther down).
- Allowed to use built in features of AWS for certain requirements.
- No automated tests (See Testing section below).

## Additional requirements chosen
- Add token-based access control to your files such that instead of the identifier, files can be accessed with a token that expires after a set period of time. 
  - When files are requested for download, I generate a pre-signed URL from S3 that expires on a default duration
- Build a web page/app that provides a browser-based mechanism for using your upload and download endpoints. 
  - While not the best looking page, the API endpoints can be viewed in a browser directly and a file can be uploaded/downloaded.

## Building

This project is based around the Maven build environment and JDK 8. You will need both of those tools available.

See <a href="http://openjdk.java.net/install/">this link</a> for installing JDK/JRE on your OS

See <a href="https://maven.apache.org/install.html">this link</a> for installing maven on your OS

**Note:** Certain platforms can also install the above using their package manager of choice (Brew, Apt, Yum, etc.). 
So you may want to install it from their if its easier.

Run the <pre> https://github.com/Rich7690/simple-file-handler.git </pre> command to checkout the project.

From the root directory of the project (pom.xml file is there) run the following <pre>mvn clean package</pre>

If you see errors saying that mvn isn't a command or unable to find Java, it means you haven't set up your path correctly to access
the files you installed above.

## Running

From the root directory of the project (pom.xml file is there) run the following <pre>mvn exec:java</pre>

By default, the application listens on port 4567.

**Note:** This application accesses AWS resources (S3 and DynamoDB) and does not include access keys along with the source
code. Thus, it needs to either run on an EC2 instance that has access to these resources or else an access key and secret key must be provided.
Environment variables can be set as follows:
**Linux, macOS, or Unix**
<pre>
$ export AWS_ACCESS_KEY_ID=ExampleID
$ export AWS_SECRET_ACCESS_KEY=ExampleKey
</pre>
**Windows**
<pre>
> set AWS_ACCESS_KEY_ID=ExampleID
> set AWS_SECRET_ACCESS_KEY=ExampleKey
</pre>

## Access

The application is running at the following URL <pre>http://ec2-52-24-74-226.us-west-2.compute.amazonaws.com:4567/</pre>
Use the form to upload files and save the returned identifier.
Download files using a GET request (paste link in browser) against <pre>http://ec2-52-24-74-226.us-west-2.compute.amazonaws.com:4567/file/\<identifier\></pre>
The response will give you a pre-signed S3 link to download directly from the file storage service

## Design / Architectural / Technical decisions

Design wise, this technical problem is mostly a simple web application that can proxy requests to a proper file storage
service (i.e. S3) along with a few aspects that require storing extra metadata.

I chose a middle ground approach to keep the design simple while also allowing it to scale well horizontally while also limiting
it in scope just to the requirements chosen (A more generic approach could be taken given many more use cases). The solution
consists of storing a key value object in DynamoDB of the form identifier -> filename so that users may get their files back
using a unique identifier rather than the file name. Then, the system will lookup the identifier in Dynamo for downloads where
we will then generate a pre-signed URL in S3 that is valid for a default time period. The webservice will return a 302
redirect with this URL so that the browser will download it with the original file name. Note that I specifically chose
DynamoDB as the data backend due to its scalability/availability over a traditional relational database. 

### Alternate approaches and improvements

A simpler approach could have avoided any external systems by just storing the files on disk and keeping ids in memory or a separate file.
This approach while simpler, does not scale and also will not be persistent if ephemeral storage is lost (EBS).

Another approach could also avoid the web server all together and use an API Gateway to AWS Lambda approach..

An improvement to be made to the current approach, would be to avoid proxying the data through this web application when
uploading files to S3. It is possible to provide signed URLs to the browser to upload directly to S3. This would reduce
connections and load to the web server and allow S3 to handle it directly.

### Implementing more requirements
Some discussion about how I would go about implementing the additional requirements given enough time:

- Add user-based access control to your files such that only the user that originally uploaded the file can access it.
  - Roll your own solution - store usernames and such yourself in DynamoDB and create folders for each user in S3
  - Use AWS services (preferred) - Use AWS Cognito to authenticate users then use a Federated ID policy to allow them access to their files only enforced at the S3 level 
(See: <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_examples_s3_cognito-bucket.html">example</a>)

- Add an endpoint that returns a list of all files in the system, their identifier, original filename, and the byte size of the file. 
  - S3 has an API that allows us to list bucket objects, but we would also need to get the other metadata as well. It makes the most sense
to create some kind of paginated API instead and lookup the additional data in an AJAX way in the browser. Return say 20-100 at a time, and then
fire off parallel requests to the backend to get whatever other metadata we need (orginal filename etc.)
  - If we really want the experience, we could store all this metadata in the DynamoDB table and just do a GSI query on it as well.

- Automate the setup of all infrastructure (servers, cloud services, code, etc) such that you could easily deploy a second complete, working copy of your app in a command or two. 
  - This sounds like a great use case for <a href="https://aws.amazon.com/cloudformation/">Cloud Formation </a> and <a href="https://aws.amazon.com/codedeploy/"> Code Deploy </a>.
  These services will auto-provision our resources and deploy the code to a new stack.

## Testing

Currently, the source code contains no automated testing. Mainly this is due to their being no mention of it in the requirements.
However, I still think it is generally important to write tests for code I will discuss that here.
- Unit Tests - under most circumstances it's appropriate to write automated unit tests to test individual units of code to verify correctness. 
An agreed upon line and branch percentage is generally accepted amongst the development team.
- Functional Tests - One of the more useful things to automate would be an external client that hits the endpoints to this web
application from the outside (block box) and verifies the functionality behaves how it should (i.e  I can download a file I upload)