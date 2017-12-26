// Folders
//def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "SampleProject"
 
// Jobs
def buildMavenJob = freeStyleJob(projectFolderName + "/Activity_Maven")
def buildSonarJob = freeStyleJob(projectFolderName + "/Activity_Sonarqube")
def buildNexusSnapshotsJob = freeStyleJob(projectFolderName + "/Activity_Nexus")
 
// Views
def pipelineView = buildPipelineView(projectFolderName + "/ProjectSimulator")
 
pipelineView.with{
    title('ProjectSimulator')
    displayedBuilds(10)
    selectedJob(projectFolderName + "/Activity_Maven")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

folder("${projectFolderName}") {
	displayName("${projectFolderName}")
	description("${projectFolderName}")
}


buildMavenJob.with{

	scm{
		git{
			remote{
				credentials('adop-jenkins-master')
				url('http://gitlab/gitlab/cailax/CurrencyConverterDTS.git')
			}
		branch('*/master')
		}
	}



	triggers {
		gitlabPush{
			buildOnMergeRequestEvents(true)
			buildOnPushEvents(true)
			enableCiSkip(true)
			setBuildDescription(false)
			rebuildOpenMergeRequest('never')
        } }
  
	wrappers {
		preBuildCleanup()
	}
  
		steps {
			maven{
				mavenInstallation('ADOP Maven')
				goals('package')
	}
}
		publishers {
			archiveArtifacts('**/*.war')
			downstream('Activity_Sonarqube','SUCCESS')
		}
	}

buildSonarJob.with{

	scm{
		git{
			remote{
				credentials('adop-jenkins-master')
				url('http://gitlab/gitlab/cailax/CurrencyConverterDTS.git')
			}
		branch('*/master')
		}
	}
configure { project ->
	project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
			properties('''sonar.projectKey=team2projkey
			sonar.projectName=Currency_Converter
			sonar.projectVersion=1
			sonar.sources=src/main/webapp''')
			javaOpts()
			jdk('(Inherit From Job)')
			task()
	}
}

		publishers {
			downstream('Activity_Nexus','SUCCESS')
		}
	}

buildNexusSnapshotJob.with{

	steps {
        copyArtifacts('Activity_Maven') {
            includePatterns('target/*.war')
            buildSelector {
                latestSuccessful(true)
            }
            fingerprintArtifacts(boolean fingerprint = true)
        }
        nexusArtifactUploader {
        nexusVersion('NEXUS2')
        protocol('HTTP')
        nexusUrl('nexus:8081/nexus')
        groupId('DTSActivity')
        version('1')
        repository('snapshots')
        credentialsId('new')
        artifact {
            artifactId('CurrencyConverter')
            type('war')
            file('/var/jenkins_home/jobs/Activity_Maven/workspace/target/CurrencyConverter.war')
        }
    }
    }
		publishers {
			archiveArtifacts('**/*.war')
		}
	}
