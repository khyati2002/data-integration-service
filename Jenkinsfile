pipeline {
    agent any

    parameters {
        string(name: 'LOB_NAME', defaultValue: 'new-lob', description: 'Unique name for the new Line of Business')
        string(name: 'ENV', defaultValue: 'dev', description: 'Target environment (e.g., dev, qa, prod)')
        string(name: 'REGION', defaultValue: 'ap-south-1', description: 'Target AWS Region')
        string(name: 'TERRAGRUNT_INPUTS', defaultValue: '{}', description: 'JSON string of terragrunt inputs to override')
        string(name: 'FLINK_PROPERTIES', defaultValue: '{}', description: 'JSON string of flink properties to override')
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    def branchName = "feature/${params.LOB_NAME}"
                    echo "Checking out branch: ${branchName}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: branchName]],
                        userRemoteConfigs: scm.userRemoteConfigs
                    ])
                }
            }
        }
        stage('Setup LOB Environment') {
            steps {
                script {
                    // Ensure Node.js is available in your Jenkins agent environment
                    sh "node scripts/setup-lob.js --lob '${params.LOB_NAME}' --env '${params.ENV}' --region '${params.REGION}' --terragrunt-inputs '${params.TERRAGRUNT_INPUTS}' --flink-properties '${params.FLINK_PROPERTIES}'"
                    
                    // Commit the generated files
                    sh "git add environments/${params.ENV}/${params.REGION}/${params.LOB_NAME}/"
                    sh "git commit -m 'feat: Add/Update LOB ${params.LOB_NAME}'"
                }
            }
        }

        stage('Deploy Infrastructure (Manual Trigger)') {
            // This stage is for demonstrating the next step.
            // You might run this automatically or have a manual approval.
            input {
                message "Deploy infrastructure for ${params.LOB_NAME}?"
                ok "Yes, deploy"
            }
            steps {
                dir("environments/${params.ENV}/${params.REGION}/${params.LOB_NAME}") {
                    // Ensure Terragrunt is installed on your Jenkins agent
                    sh 'terragrunt run-all apply --terragrunt-non-interactive'
                }
            }
        }
    }
}
