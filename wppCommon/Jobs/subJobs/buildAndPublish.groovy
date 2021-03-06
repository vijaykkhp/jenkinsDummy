//incoming parameters
//def url = "https://github.com/WPPg2/avatar-reg"
//def gitCredentials = "8cf0000b-3991-4db0-a2d9-e157168d2cef"
//def serviceName = "avreg"
//def buildTrigger = "NIGHTLY"
//def targetNode = 'Morpheus'

def workspaceDir
def pom
def version
def groupID
def artifactVersion
def repoURL
def buildTimestamp = new Date().format("MMM.dd.yyyy.hh.mm.aaa") //e.g. May.26.2016.12.32.AM
def mvnpath = "/opt/apache-maven-3.2.5/bin/mvn"
    
node(targetNode){
    //get current workspace
    workspaceDir = pwd()
	
    //git checkout
    git credentialsId: "$gitCredentials", url: "$url"
    
	//maven evaluate POM values
    sh """
    ${mvnpath} -f ${workspaceDir}/${rootPomPath} org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.version  | grep Building | cut -d' ' -f4 > maven.version
    ${mvnpath} -f ${workspaceDir}/${rootPomPath} org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.groupId  | grep -v INFO | grep -i -v WARNING > ${workspaceDir}/maven.groupId
	${mvnpath} -f ${workspaceDir}/${rootPomPath} org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.artifactId  | grep -v INFO | grep -i -v WARNING > ${workspaceDir}/maven.artifactId
	${mvnpath} -f ${workspaceDir}/${rootPomPath} org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.distributionManagement.repository.url  | grep -v INFO | grep -i -v WARNING > ${workspaceDir}/maven.repoUrl"""
    
    groupID = readFile file: "${workspaceDir}/maven.groupId"
	artifactID = readFile file: "${workspaceDir}/maven.artifactId"
	version = readFile file: "${workspaceDir}/maven.version"
	repoURL = readFile file: "${workspaceDir}/maven.repoUrl"
	
	groupID=groupID.trim()
	groupIdSlashed = groupID.replaceAll('\\.','/')
	artifactID=artifactID.trim()
	version=version.trim()
	repoURL = repoURL.trim()

    artifactVersion = "${version}-${buildTrigger}-${buildTimestamp}"
    echo "version ${version}"
    echo "groupID ${groupID}"
    echo "artifactID ${artifactID}"
    echo "repoURL ${repoURL}"

    
    //pre-build steps    
    sh "${mvnpath} -f ${workspaceDir}/${rootPomPath}/pom.xml clean versions:set -DnewVersion=${artifactVersion}"
    sh "git rev-parse HEAD > ./git.sha"
    def commitID = readFile file: "./git.sha"
    commitID =commitID.trim()
    println "commit_id is ${commitID}"

    
    
    //build and deploy step
    sh "${mvnpath} -B -f ${workspaceDir}/${rootPomPath}/pom.xml deploy cobertura:cobertura -Dcobertura.report.format=xml"
    
    def fileName = "${serviceName}-restapp-${artifactVersion}.war"
    def artifactURL = "${repoURL}/${groupIdSlashed}/${serviceName}-restapp/${artifactVersion}/${fileName}"
    println artifactURL
	
	//return values - artifactURL, artifactVersion, commitID
    currentBuild.setDescription("#artifactURL="+artifactURL+"#artifactVersion="+artifactVersion+"#commitID="+commitID)
}



