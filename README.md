# AWS 및 Jenkins를 활용한 CI/CD 파이프라인 구축

<h2 style="font-size: 25px;"> TEAM 👨‍👨‍👧 <br>
</h2>

|<img src="https://avatars.githubusercontent.com/u/81280628?v=4" width="100" height="100"/>|<img src="https://avatars.githubusercontent.com/u/86951396?v=4" width="100" height="100"/>|<img src="https://avatars.githubusercontent.com/u/139302518?v=4" width="100" height="100"/>|<img src="https://avatars.githubusercontent.com/u/78792358?v=4" width="100" height="100"/>
|:-:|:-:|:-:|:-:|
|[@손대현](https://github.com/DaeHyeonSon)|[@이아영](https://github.com/ayleeee)|[@곽병찬](https://github.com/gato-46)|[@박현우](https://github.com/smartcow99)
---


## 개요 📝
이 프로젝트는 Jenkins를 Docker 환경에 설치하고 AWS를 통해 CI/CD 파이프라인을 구성하는 방법에 대해 설명합니다. Jenkins를 사용해 소스 코드를 빌드하고, AWS S3에 빌드 파일을 업로드한 후, EC2 인스턴스에 배포하는 전 과정을 자동화한다. 이를 통해 소프트웨어의 배포 시간을 단축하고, 효율적인 배포 프로세스를 구성할 수 있다.

또한, 물리적인 인프라를 함께 사용함으로써 **AWS 클라우드 리소스 사용량을 최적화**하고, **비용을 줄일 수 있는 이점**도 있다. 빌드 및 테스트와 같은 무거운 작업은 로컬 또는 사내 인프라에서 처리하고, 배포와 같은 최종 단계만 AWS를 활용하는 방식으로 클라우드 비용을 절감할 수 있다.

### 사용 기술 💻
- **AWS:** EC2, S3, RDS
- **CI/CD:** Jenkins, Docker
- **Version Control:** GitHub
- **기타:** Ngrok, Inotify, Shell Script
  

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

### 4. AWS S3에 업로드 ☁️
빌드된 .jar 파일을 AWS S3에 업로드하여 배포 준비를 마칩니다.

### 5. EC2에 배포 🖥️
S3에서 EC2 인스턴스로 파일을 전송하여 애플리케이션을 배포합니다.

<br>

## Trouble Shooting 🔥

| 이 프로젝트에서는 여러 가지 기술을 사용하면서 발생한 이슈들과 그 해결 방법을 정리하였습니다.

### 1. **Application.properties 은닉 관련 이슈 🔒**
   - **문제:** AWS EC2 및 RDS 관련 개인정보를 GitHub에 올릴 수 없기에 Jenkins에서 환경변수로 관리하는 방법을 선택했습니다. 이 과정에서 빌드 파일 실행 시 환경변수를 추가해주는 옵션을 반드시 추가해야 한다는 문제가 발생했습니다.
   - **해결 방법:** Jenkins의 **환경 변수 설정**을 통해 비밀 정보를 안전하게 관리하고, 빌드 스크립트에서 해당 환경변수를 참조하도록 수정했습니다.

### 2. **관리자 권한 이슈 🔑**
   - **문제:** 빌드 파일 실행, 프로세스 실행 검사, 프로세스 종료 등 여러 명령어 실행 시 지속적으로 관리자 권한이 필요했습니다. 이로 인해 여러 번의 오류가 발생하고 시간 소모가 있었습니다.
   - **해결 방법:** 필요한 권한을 부여하여 스크립트가 문제 없이 실행될 수 있도록 설정했습니다.

### 3. **Shell Script 문법 이슈 🐚**
   - **문제:** Shell Script의 띄어쓰기나 단락 들여쓰기 문제로 인해 지속적인 오류가 발생했습니다. 스크립트 작성 시 작은 실수로 인해 큰 문제가 발생할 수 있음을 깨달았습니다.
   - **해결 방법:** 스크립트를 작성한 후, 코드 리뷰 및 철저한 확인 과정을 통해 문법 오류를 수정했습니다.

### 4. **Inotify 파일 수정 종류 이슈 🔄**
   - **문제:** S3에서 파일을 받아오는 과정이 기존 파일에 대한 수정이 아닌 새로운 파일 생성이라는 사실을 여러 번의 테스트를 통해 알게 되었습니다. 이는 inotify가 기존 파일의 수정이 아닌 파일 생성 이벤트를 감지하도록 수정해야 함을 의미했습니다.
   - **해결 방법:** `inotifywait`를 사용하여 폴더 내부의 변화(파일 생성)를 감지하도록 스크립트를 변경했습니다.

### 5. **AWS 권한 이슈 ☁️**
   - **문제:** `.pem` 키페어에 쓰기 권한이 설정되어 있을 경우, SSH 연결 시 오류가 발생했습니다. 이로 인해 AWS CLI를 통한 작업이 실패했습니다.
   - **해결 방법:** `chmod 400` 명령어를 사용하여 키 파일의 권한을 제한하고, Jenkins AWS Credentials Plugin을 통해 개인 정보를 안전하게 보호하며 AWS CLI 권한을 획득했습니다.

### 6. **Docker 바인드 이슈 🐳**
   - **문제:** Docker 이미지와 기존 Ubuntu 환경의 파일 공유를 위해 Docker 바인드를 사용해야 했습니다. 이 과정에서 경로 설정이나 권한 문제로 어려움이 있었습니다.
   - **해결 방법:** Docker의 `-v` 옵션을 사용하여 호스트의 디렉토리를 컨테이너와 연결하고, 권한을 적절하게 설정하여 파일 공유 문제를 해결했습니다.

---


### 🎯 결론
AWS와 Jenkins를 활용한 CI/CD 파이프라인은 애플리케이션 개발 및 배포 시간을 단축하고, 품질을 보장하는 데 필수적이다. 또한, 물리적 인프라와 AWS 클라우드를 조합한 하이브리드 접근 방식을 통해 비용 절감과 함께 유연한 인프라 운영이 가능하며, 이를 통해 최적의 효율을 달성할 수 있다.

이러한 시스템을 통해 개발팀은 민첩하고 안정적인 개발 환경을 제공받고, 기업은 클라우드 비용을 효율적으로 관리할 수 있다.
