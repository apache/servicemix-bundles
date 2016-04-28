#!/bin/sh


PARENT_VER=9
OPENSAML_VER=3.2.0
JAVASUPPORT_VER=7.2.0
SPRINGEXT_VER=5.2.0


download()
{
BASEURL=https://build.shibboleth.net/nexus/content/repositories/releases
COMPONENTS="opensaml-core
    opensaml-messaging-api
    opensaml-messaging-impl
    opensaml-parent
opensaml-profile-api
opensaml-profile-impl
opensaml-saml-api
opensaml-saml-impl
opensaml-security-api
opensaml-security-impl
opensaml-soap-api
opensaml-soap-impl
opensaml-storage-api
opensaml-storage-impl
opensaml-xacml-api
opensaml-xacml-impl
opensaml-xacml-saml-api
opensaml-xacml-saml-impl
opensaml-xmlsec-api
opensaml-xmlsec-impl
"


for component in $COMPONENTS
do
   wget -e robots=off --no-check-certificate -np -r -nH "$BASEURL/org/opensaml/$component/$OPENSAML_VER/"
done

wget -e robots=off --no-check-certificate -np -r -nH "$BASEURL/net/shibboleth/parent-v3/$PARENT_VER/"
wget -e robots=off --no-check-certificate -np -r -nH "$BASEURL/net/shibboleth/utilities/java-support/$JAVASUPPORT_VER/"
wget -e robots=off --no-check-certificate -np -r -nH "$BASEURL/net/shibboleth/ext/spring-extensions/$SPRINGEXT_VER/"

find . -name "index.html" | xargs rm
}

# remove the repository entries
removeRepositories()
{
POMS="
 nexus/content/repositories/releases/org/opensaml/opensaml-parent/$OPENSAML_VER/opensaml-parent-$OPENSAML_VER.pom
 nexus/content/repositories/releases/net/shibboleth/utilities/java-support/$JAVASUPPORT_VER/java-support-$JAVASUPPORT_VER.pom
 nexus/content/repositories/releases/net/shibboleth/parent-v3/$PARENT_VER/parent-v3-$PARENT_VER.pom"

for pom in $POMS
do
xsltproc -o $pom removeRepos.xsl $pom
rm $pom.asc*
rm $pom.sha1
rm $pom.md5

md5 -q < $pom > $pom.md5
shasum $pom  | colrm 42 > $pom.sha1

gpg --detach-sign -a $pom
done

}


#test
testPoms()
{
cp -a nexus/content/repositories/releases/* ~/.m2/repository/
find . -name "*.pom" -exec mvn -N -f {} dependency:tree \; | tee output_deps.txt | grep BUILD
}


download
removeRepositories
testPoms

echo "Check up and look for failures"
read -p "Press any key to continue... " -n1 -s
echo "Use the commented out commands in the script to do the rest"


USERNAMEPASSWORD=dkulp:XXXXXXXXXX

#Open a staging repo
#curl -X POST -d @OpenSAMLStartStaging.xml -u $USERNAMEPASSWORD -H "Content-Type:application/xml" -v https://oss.sonatype.org/service/local/staging/profiles/49d3626a5c71/start

#In the XML returned from above is the repo id that is needed
#REPO_ID=orgopensaml-1013
#cd nexus/content/repositories/releases/
# Upload all the artifacts to that repo
#find . -type f -exec curl -v -u $USERNAMEPASSWORD --upload-file {} https://oss.sonatype.org/service/local/staging/deployByRepositoryId/$REPO_ID/{} \;
# Login to oss and close the repo
