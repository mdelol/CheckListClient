import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2019.1"

project {

    buildType(LukkaDeploy)
    buildType(LukkaBuild)
}

object LukkaBuild : BuildType({
    name = "Lukka build"

    artifactRules = "target/install => target/install"

    params {
        text("version.snapshot", "", description = "snapshot version that the project will have after build", display = ParameterDisplay.PROMPT,
              regex = "[0-9]+")
        text("version.new", "", description = "version that is going to be built (next version will be {VERSON+1}-SNAPSHOT", display = ParameterDisplay.PROMPT,
              regex = "[0-9]+")
    }

    vcs {
        root(AbsoluteId("DxFeed_Lukka_SshGitStashInDevexpertsCom7999lukkaLibraGitRefsHeadsMaster"))

        branchFilter = ""
        excludeDefaultBranchChanges = true
    }

    steps {
        maven {
            name = "Test"
            goals = "clean test"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
        }
        maven {
            name = "Set version to release"
            goals = "-DgenerateBackupPoms=false versions:set -DnewVersion=%version.new%"
        }
        maven {
            name = "Build artifacts"
            goals = "install"
        }
        script {
            name = "Update Release Notes"
            scriptContent = """sed -i '1s/^/build libra-%version.new%\n/' ReleaseNotes.txt"""
        }
        script {
            name = "Upload artifacts"
            scriptContent = """
                set -e
                
                mkdir /mnt/projects/mdd/libra/release/%version.new%
                cp target/install/* /mnt/projects/mdd/libra/release/%version.new%/
            """.trimIndent()
        }
        script {
            name = "Commit new version"
            scriptContent = """
                git status
                git add -u
                git status
                git commit -m'build%version.new%'
                git push
                git tag build%version.new%
                git push origin build%version.new%
            """.trimIndent()
        }
        maven {
            name = "Set version to snapshot"
            goals = "-DgenerateBackupPoms=false versions:set -DnewVersion=%version.snapshot%-SNAPSHOT"
        }
        script {
            name = "Commit snapshot"
            scriptContent = """
                git status
                bash -c 'git add "*pom.xml"'
                git status
                git commit -m'build%version.snapshot%-SNAPSHOT'
                git push
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = ""
            enableQueueOptimization = false
        }
    }
})

object LukkaDeploy : BuildType({
    name = "Lukka Deploy"
    description = "Deploys an already built version"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        text("release.version", "%dep.DxFeed_Lukka_LukkaBuild.version.new%", label = "Version to deploy", description = "Version to deploy",
              regex = "[0-9]+")
        param("dep.DxFeed_Lukka_LukkaBuild.version.new", "44")
        text("LUKKA_2_SERVICES", """("bitflyer" "huobi" "rawuploader")""", label = "list of services to be deployed to second server", description = "list of services to be deployed to second server", display = ParameterDisplay.HIDDEN, allowEmpty = true)
        text("LUKKA_1_SERVICES", """( "binance" "bitmex" "bitstamp" "bittrex" "coincap" "cxfinex bit" "cxok coin" "cxok ex" "gdax" "gemini" "hbdm" "hitbtc" "kbe" "poloniex" "rawuploader" )""", display = ParameterDisplay.HIDDEN, allowEmpty = true)
    }

    steps {
        step {
            name = "Upload artifacts to first server"
            type = "ssh-deploy-runner"
            param("jetbrains.buildServer.deployer.username", "lukkastaging")
            param("teamcitySshKey", "New QA ssh key")
            param("jetbrains.buildServer.deployer.sourcePath", "/mnt/projects/mdd/libra/release/%release.version%/*")
            param("jetbrains.buildServer.deployer.targetUrl", "lukka-qa1.aws.mdd.lo:releases/%release.version%")
            param("jetbrains.buildServer.sshexec.authMethod", "UPLOADED_KEY")
            param("jetbrains.buildServer.deployer.ssh.transport", "jetbrains.buildServer.deployer.ssh.transport.scp")
        }
        step {
            name = "Upload artifacts to second server"
            type = "ssh-deploy-runner"
            param("jetbrains.buildServer.deployer.username", "lukkastaging")
            param("teamcitySshKey", "New QA ssh key")
            param("jetbrains.buildServer.deployer.sourcePath", "/mnt/projects/mdd/libra/release/%release.version%/*")
            param("jetbrains.buildServer.deployer.targetUrl", "lukka-qa2.aws.mdd.lo:releases/%release.version%")
            param("jetbrains.buildServer.sshexec.authMethod", "UPLOADED_KEY")
            param("jetbrains.buildServer.deployer.ssh.transport", "jetbrains.buildServer.deployer.ssh.transport.scp")
        }
        step {
            name = "Deploy services to first server"
            type = "ssh-exec-runner"
            param("jetbrains.buildServer.deployer.username", "lukkastaging")
            param("jetbrains.buildServer.sshexec.command", """
                echo '#!/bin/bash' > /tmp/deploy_script.sh
                echo 'TO_RELEASE=%LUKKA_1_SERVICES% ' >> /tmp/deploy_script.sh
                echo 'for ((i = 0; i < ${'$'}{#TO_RELEASE[@]}; i++))' >> /tmp/deploy_script.sh
                echo 'do' >> /tmp/deploy_script.sh
                echo '    if [[ ${'$'}(echo "${'$'}{TO_RELEASE[${'$'}i]}" | wc -w) == 1 ]] ' >> /tmp/deploy_script.sh
                echo '    then' >> /tmp/deploy_script.sh
                echo '        SERVICE="${'$'}{TO_RELEASE[${'$'}i]}"' >> /tmp/deploy_script.sh
                echo '        NODE=' >> /tmp/deploy_script.sh
                echo '    else        ' >> /tmp/deploy_script.sh
                echo '        SERVICE=${'$'}(echo "${'$'}{TO_RELEASE[${'$'}i]}" | cut -d " " -f1)' >> /tmp/deploy_script.sh
                echo '        NODE=${'$'}(echo "${'$'}{TO_RELEASE[${'$'}i]}" | cut -d " " -f2)' >> /tmp/deploy_script.sh
                echo '    fi' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '    echo "---------------starting to process service \"${'$'}SERVICE\" with node \"${'$'}NODE\"---------------"' >> /tmp/deploy_script.sh
                echo '#Stopping service ' >> /tmp/deploy_script.sh
                echo '    echo "---------------Stopping ${'$'}{TO_RELEASE[${'$'}i]}-------------------------------------------------"' >> /tmp/deploy_script.sh
                echo '    ~/binng/stop "${'$'}{SERVICE}" "${'$'}{NODE}"' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '#moving old version to backup' >> /tmp/deploy_script.sh
                echo '    echo "---------------Backuping old version of ${'$'}{TO_RELEASE[${'$'}i]}----------------------------------"' >> /tmp/deploy_script.sh
                echo '    rm -rf ~/_backup/${'$'}{SERVICE}' >> /tmp/deploy_script.sh
                echo '    mv -v ~/"${'$'}{SERVICE}" ~/_backup' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '#unzipping the new version' >> /tmp/deploy_script.sh
                echo '    echo "---------------Unzipping new version of ${'$'}{SERVICE}----------------------------------"' >> /tmp/deploy_script.sh
                echo '    CF="releases/%release.version%/${'$'}{SERVICE}.zip"' >> /tmp/deploy_script.sh
                echo '    echo "file to unzip: ${'$'}{CF}"' >> /tmp/deploy_script.sh
                echo '    unzip -d ~/ "${'$'}{CF}"' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '#deploying the new version' >> /tmp/deploy_script.sh
                echo '    echo "---------------Starting ${'$'}{TO_RELEASE[${'$'}i]}-------------------------------------------------"' >> /tmp/deploy_script.sh
                echo '    yes | ~/binng/start "${'$'}{SERVICE}" "${'$'}{NODE}"' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo 'done' >> /tmp/deploy_script.sh
                
                bash /tmp/deploy_script.sh
                rm -rf /tmp/deploy_script.sh
            """.trimIndent())
            param("teamcitySshKey", "New QA ssh key")
            param("jetbrains.buildServer.deployer.targetUrl", "lukka-qa1.aws.mdd.lo")
            param("jetbrains.buildServer.sshexec.authMethod", "UPLOADED_KEY")
        }
        step {
            name = "Deploy services to second server"
            type = "ssh-exec-runner"
            param("jetbrains.buildServer.deployer.username", "lukkastaging")
            param("jetbrains.buildServer.sshexec.command", """
                echo '#!/bin/bash' > /tmp/deploy_script.sh
                echo 'TO_RELEASE=%LUKKA_2_SERVICES% ' >> /tmp/deploy_script.sh
                echo 'for ((i = 0; i < ${'$'}{#TO_RELEASE[@]}; i++))' >> /tmp/deploy_script.sh
                echo 'do' >> /tmp/deploy_script.sh
                echo '    if [[ ${'$'}(echo "${'$'}{TO_RELEASE[${'$'}i]}" | wc -w) == 1 ]] ' >> /tmp/deploy_script.sh
                echo '    then' >> /tmp/deploy_script.sh
                echo '        SERVICE="${'$'}{TO_RELEASE[${'$'}i]}"' >> /tmp/deploy_script.sh
                echo '        NODE=' >> /tmp/deploy_script.sh
                echo '    else        ' >> /tmp/deploy_script.sh
                echo '        SERVICE=${'$'}(echo "${'$'}{TO_RELEASE[${'$'}i]}" | cut -d " " -f1)' >> /tmp/deploy_script.sh
                echo '        NODE=${'$'}(echo "${'$'}{TO_RELEASE[${'$'}i]}" | cut -d " " -f2)' >> /tmp/deploy_script.sh
                echo '    fi' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '    echo "---------------starting to process service \"${'$'}SERVICE\" with node \"${'$'}NODE\"---------------"' >> /tmp/deploy_script.sh
                echo '#Stopping service ' >> /tmp/deploy_script.sh
                echo '    echo "---------------Stopping ${'$'}{TO_RELEASE[${'$'}i]}-------------------------------------------------"' >> /tmp/deploy_script.sh
                echo '    ~/binng/stop "${'$'}{SERVICE}" "${'$'}{NODE}"' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '#moving old version to backup' >> /tmp/deploy_script.sh
                echo '    echo "---------------Backuping old version of ${'$'}{TO_RELEASE[${'$'}i]}----------------------------------"' >> /tmp/deploy_script.sh
                echo '    rm -rf ~/_backup/${'$'}{SERVICE}' >> /tmp/deploy_script.sh
                echo '    mv -v ~/"${'$'}{SERVICE}" ~/_backup' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '#unzipping the new version' >> /tmp/deploy_script.sh
                echo '    echo "---------------Unzipping new version of ${'$'}{SERVICE}----------------------------------"' >> /tmp/deploy_script.sh
                echo '    CF="releases/%release.version%/${'$'}{SERVICE}.zip"' >> /tmp/deploy_script.sh
                echo '    echo "file to unzip: ${'$'}{CF}"' >> /tmp/deploy_script.sh
                echo '    unzip -d ~/ "${'$'}{CF}"' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo '#deploying the new version' >> /tmp/deploy_script.sh
                echo '    echo "---------------Starting ${'$'}{TO_RELEASE[${'$'}i]}-------------------------------------------------"' >> /tmp/deploy_script.sh
                echo '    yes | ~/binng/start "${'$'}{SERVICE}" "${'$'}{NODE}"' >> /tmp/deploy_script.sh
                echo '' >> /tmp/deploy_script.sh
                echo 'done' >> /tmp/deploy_script.sh
                
                bash /tmp/deploy_script.sh
                rm -rf /tmp/deploy_script.sh
            """.trimIndent())
            param("teamcitySshKey", "New QA ssh key")
            param("jetbrains.buildServer.deployer.targetUrl", "lukka-qa2.aws.mdd.lo")
            param("jetbrains.buildServer.sshexec.authMethod", "UPLOADED_KEY")
        }
    }

    triggers {
        finishBuildTrigger {
            enabled = false
            buildType = "${LukkaBuild.id}"
            successfulOnly = true
        }
    }
})
