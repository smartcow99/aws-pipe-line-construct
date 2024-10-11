# aws-pipe-line-construct
AWS CI/CD Construct

# AWS 및 Jenkins를 활용한 CI/CD 파이프라인 구축
## 개요 📝
이 프로젝트는 Jenkins를 Docker 환경에 설치하고 AWS를 통해 CI/CD 파이프라인을 구성하는 방법에 대해 설명합니다. Jenkins를 사용해 소스 코드를 빌드하고, AWS S3에 빌드 파일을 업로드한 후, EC2 인스턴스에 배포하는 전 과정을 자동화한다. 이를 통해 소프트웨어의 배포 시간을 단축하고, 효율적인 배포 프로세스를 구성할 수 있다.

## 과정 🚀
**1. Docker로 Jenkins 설치** <br>
**2. Jenkins 컨테이너 실행 ⚙️** <br>
**3. Jenkins 파이프라인 설정 🔧** <br>
**4. AWS S3에 업로드 ☁️** <br>
**5. EC2에 배포 🖥️** <br>

**1. Docker로 Jenkins 설치** <br>
먼저, Jenkins를 Docker로 실행하여 빠르게 CI/CD 환경을 구축한다.

```bash
# apt 인덱스 업데이트
sudo apt-get update
sudo apt-get install ca-certificates curl gnupg lsb-release

# Docker 공식 GPG 키 추가
sudo mkdir -m 0755 -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Docker 저장소를 APT 소스에 추가
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# APT 패키지 캐시 업데이트 및 Docker 설치
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 사용자 권한 설정
sudo usermod -aG docker $USER
newgrp docker  # 적용을 위해 로그아웃하거나 재부팅 필요
```

<br>

**2. Jenkins 컨테이너 실행 ⚙️**
Jenkins의 홈 디렉토리를 볼륨으로 마운트하여 지속적인 저장소를 구성합니다.

```bash
docker run --name myjenkins --privileged -p 8080:8080 -v $(pwd)/appjardir:/var/jenkins_home/appjar jenkins/jenkins:lts-jdk17
```

<br>

**3. Jenkins 파이프라인 설정 🔧**
파이프라인 스크립트를 통해 GitHub 리포지토리에서 코드를 클론하고, Gradle을 사용하여 빌드한 후 AWS S3에 업로드하고, EC2 인스턴스에 배포한다.

```groovy
pipeline {
    agent any
    environment {
        DB_URL = 'database_url'
        DB_USER = 'username'
        DB_PW = 'password'
        AWS_REGION = 'region'
        AWS_CREDENTIALS = credentials('aws_id')
        EC2_KEY_PATH = '/key/path'
        EC2_USER = 'ubuntu'
        EC2_HOST = 'host_ip'
        S3_BUCKET = 'bucket_name'
        JAR_FILE = 'file_name'
    }
    stages {
        stage('Clone Repository') {
            steps {
                git branch: 'main', url: 'github_url'
            }
        }
        stage('Build') {   
            steps {
                dir('./demoApp') {                   
                    sh './gradlew clean build -x test'
                }
            }
        }
        stage('Copy JAR') {
            steps {
                script {
                    def jarFile = 'demoApp/build/libs/demoApp-0.0.1-SNAPSHOT.jar'
                    def targetDir = '/var/jenkins_home/appjar/'
                    sh "cp ${jarFile} ${targetDir}"
                }
            }
        }
        stage('Upload to S3') {
            steps {
                script {
                    sh """
                        aws s3 cp /var/jenkins_home/appjar/demoApp-0.0.1-SNAPSHOT.jar s3://${S3_BUCKET}/
                    """
                }
            }
        }
        stage('Deploy to EC2') {
            steps {
                script {
                    sh """
                        ssh -i ${EC2_KEY_PATH} ${EC2_USER}@${EC2_HOST} "aws s3 cp s3://${S3_BUCKET}/${JAR_FILE} /home/${EC2_USER}/app/${JAR_FILE}"
                    """
                }
            }
        }
    }
}
```

<br>

4. AWS S3에 업로드 ☁️
빌드된 .jar 파일을 AWS S3에 업로드하여 배포 준비를 마칩니다.

5. EC2에 배포 🖥️
S3에서 EC2 인스턴스로 파일을 전송하여 애플리케이션을 배포합니다.

🎯 결론
Jenkins와 AWS를 활용한 CI/CD 파이프라인은 배포 프로세스를 자동화하고 신속하게 제공할 수 있는 강력한 도구입니다. 이 파이프라인은 개발자들이 코드를 안전하게 배포하고, 시간이 소모되는 수동 작업을 줄일 수 있도록 지원합니다. 💡
