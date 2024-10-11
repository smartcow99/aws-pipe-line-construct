# AWS 및 Jenkins를 활용한 CI/CD 파이프라인 구축

## 개요 📝
이 프로젝트는 Jenkins를 Docker 환경에 설치하고 AWS를 통해 CI/CD 파이프라인을 구성하는 방법에 대해 설명합니다. Jenkins를 사용해 소스 코드를 빌드하고, AWS S3에 빌드 파일을 업로드한 후, EC2 인스턴스에 배포하는 전 과정을 자동화한다. 이를 통해 소프트웨어의 배포 시간을 단축하고, 효율적인 배포 프로세스를 구성할 수 있다.

또한, 물리적인 인프라를 함께 사용함으로써 **AWS 클라우드 리소스 사용량을 최적화**하고, **비용을 줄일 수 있는 이점**도 있다. 빌드 및 테스트와 같은 무거운 작업은 로컬 또는 사내 인프라에서 처리하고, 배포와 같은 최종 단계만 AWS를 활용하는 방식으로 클라우드 비용을 절감할 수 있다.


## 과정 🚀
**1. Docker로 Jenkins 설치** <br>
**2. Jenkins 컨테이너 실행 ⚙️** <br>
**3. Jenkins 파이프라인 설정 🔧** <br>
**4. AWS S3에 업로드 ☁️** <br>
**5. EC2에 배포 🖥️** <br>

### 1. Docker로 Jenkins 설치
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

### 2. Jenkins 컨테이너 실행 ⚙️
Jenkins의 홈 디렉토리를 볼륨으로 마운트하여 지속적인 저장소를 구성한다.

```bash
docker run --name myjenkins --privileged -p 8080:8080 -v $(pwd)/appjardir:/var/jenkins_home/appjar jenkins/jenkins:lts-jdk17
```

<br>

### 3. Jenkins 파이프라인 설정 🔧
파이프라인 스크립트를 통해 GitHub 리포지토리에서 코드를 클론하고, Gradle을 사용하여 빌드한 후 AWS S3에 업로드하고, EC2 인스턴스에 배포한다.

`environment` 블록은 Jenkins Pipeline에서 전역적으로 사용할 환경 변수를 정의하는 섹션이다. 각 변수가 지정된 값을 파이프라인 전반에 걸쳐 사용되며, 특히 **AWS 관련 자격 증명**, **데이터베이스 정보**, **EC2 배포**와 같은 외부 서비스와의 통신에 중요한 역할을 한다.

```groovy
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
```

`git` 명령으로 main 브랜치의 리포지토리를 clone을 진행한 뒤 `gradlew` 명령어로 테스트 단계를 제외하고 빌드를 수행한다. 그 다음 빌드된 .jar 파일을 지정된 경로로`(/var/jenkins_home/appjar/)` 복사 진행한다. 

```groovy
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
```

이 스테이지는 Jenkins 서버에 있는 빌드된 .jar 파일을 AWS S3에 업로드하여, 이후 다른 서비스나 EC2 인스턴스에서 사용할 수 있도록 한다. 해당 작업을 통해애플리케이션 파일을 안전하게 저장하고, 원격 서버로 전달할 수 있다.

AWS CLI의 s3 cp 명령을 사용하여 로컬 경로`(/var/jenkins_home/appjar/demoApp-0.0.1-SNAPSHOT.jar)`에 있는 파일을 S3 버킷`(s3://${S3_BUCKET}/)`으로 복사합니다. `${S3_BUCKET}`은 파이프라인의 환경 변수로 설정된 S3 버킷의 이름을 참조한다.

```groovy
stage('Upload to S3') {
            steps {
                script {
                    sh """
                        aws s3 cp /var/jenkins_home/appjar/demoApp-0.0.1-SNAPSHOT.jar s3://${S3_BUCKET}/
                    """
                }
            }
        }
```

이 스테이지는 Jenkins 파이프라인에서 S3에 업로드된 .jar 파일을 EC2 인스턴스에 배포하는 작업을 수행한다. EC2 인스턴스에 SSH로 접속한 후, S3에서 .jar 파일을 다운로드하여 배포하는 방식이다. 

AWS EC2 인스턴스에 SSH로 접속하여, S3에서 빌드된 애플리케이션 파일을 다운로드하고 배포하는 단계이다.

```groovy
stage('Deploy to EC2') {
            steps {
                script {
                    sh """
                        ssh -i ${EC2_KEY_PATH} ${EC2_USER}@${EC2_HOST} "aws s3 cp s3://${S3_BUCKET}/${JAR_FILE} /home/${EC2_USER}/app/${JAR_FILE}"
                    """
                }
            }
        }
```

`Deploy to EC2` 스테이지의 주요 기능은 다음과 같다:

`EC2 접속`: Jenkins가 EC2 인스턴스에 SSH로 연결하여 원격 작업을 수행할 수 있도록 설정되어 있다.

`S3에서 파일 다운로드`: S3 버킷에 저장된 빌드된 .jar 파일을 EC2 인스턴스에 다운로드하여, 애플리케이션 배포 준비를 완료한다.

`자동화된 배포`: 이 과정을 통해 수동으로 EC2 인스턴스에 접속할 필요 없이 Jenkins 파이프라인에서 자동으로 애플리케이션 배포가 이뤄진다.


<details>
<summary>전체 코드 </summary>
<div markdown="1">

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

</div>
</details>


<br>

### 4. AWS S3에 업로드 ☁️
빌드된 .jar 파일을 AWS S3에 업로드하여 배포 준비를 마칩니다.

### 5. EC2에 배포 🖥️
S3에서 EC2 인스턴스로 파일을 전송하여 애플리케이션을 배포합니다.

### Trouble Shooting 🔥



### 🎯 결론
AWS와 Jenkins를 활용한 CI/CD 파이프라인은 애플리케이션 개발 및 배포 시간을 단축하고, 품질을 보장하는 데 필수적입니다. 또한, 물리적 인프라와 AWS 클라우드를 조합한 하이브리드 접근 방식을 통해 비용 절감과 함께 유연한 인프라 운영이 가능하며, 이를 통해 최적의 효율을 달성할 수 있습니다.

이러한 시스템을 통해 개발팀은 민첩하고 안정적인 개발 환경을 제공받고, 기업은 클라우드 비용을 효율적으로 관리할 수 있습니다.
