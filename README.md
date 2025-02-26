# FeedPing
> RSS 피드를 통한 스마트한 정보 구독 서비스 📬

<!-- ![FeedPing Banner](https://github.com/user-attachments/assets/3cad9d3c-2a55-4b1e-bb48-5d89e712a7ba) -->

![FeedPing Banner](https://github.com/user-attachments/assets/cf001243-71a8-4188-88d6-b2709b3eddcc)

<br/>

## 📌 프로젝트 소개
바쁜 일상 속에서 자주 방문하는 사이트들의 최신 소식을 확인하는 일은 번거로울 수 있습니다.  
**FeedPing**은 RSS 피드를 활용하여 사용자가 관심 있는 사이트의 새로운 글을 이메일로 알려주는 서비스입니다.

### 핵심 기능
- 📮 **이메일 알림 서비스**: 새 글이 등록되면 실시간으로 이메일 알림 발송
  
- 🔄 **중복 알림 방지**: 스마트 필터링으로 같은 콘텐츠에 대한 중복 알림 제거
- 🔍 **간편한 구독 관리**: 웹 인터페이스를 통한 쉬운 RSS 피드 구독 관리
- 📱 **원클릭 설정**: 이메일 인증 한 번으로 간편하게 시작

### 서비스 링크

<p align="center">
  <a href="https://feedping.co.kr" target="_blank" 
     style="background-color: #3B82F6; color: #fff; padding: 16px 32px; font-size: 18px; font-weight: bold; border-radius: 12px; text-decoration: none; display: inline-block;">
    FeedPing 웹사이트 방문하기
  </a>
</p>

<br/>

## ⭐️ 주요 기능

<div align="center" style="display: flex; flex-wrap: wrap; gap: 24px; justify-content: center;">
  
  <!-- 1. 인기 RSS 피드 확인 및 등록 -->
  <div style="max-width: 220px; text-align: center; background: #f9f9f9; border-radius: 8px; padding: 16px;">
    <h3 style="margin: 12px 0 8px;">인기 RSS 피드 확인 및 등록</h3>
    <p style="font-size: 14px; line-height: 1.4;">
      인기 RSS 피드를 한눈에 보고<br/>
      바로 등록해보세요
    </p>
    <img src="https://github.com/user-attachments/assets/d0f7c815-2038-4aa5-8587-56dbd574d6ed"
         alt="인기 RSS 피드 확인 및 등록"
         style="width: 100%; border-radius: 8px; margin-top: 8px;" />
  </div>

  <!-- 2. RSS 피드 등록 -->
  <div style="max-width: 220px; text-align: center; background: #f9f9f9; border-radius: 8px; padding: 16px;">
    <h3 style="margin: 12px 0 8px;">RSS 피드 등록</h3>
    <p style="font-size: 14px; line-height: 1.4;">
      관심있는 사이트의<br/>
      RSS 주소 등록
    </p>
    <img src="https://github.com/user-attachments/assets/7e872088-6e74-4ac2-9f78-207b6fc5d878"
         alt="RSS 피드 등록"
         style="width: 50%; border-radius: 8px; margin-top: 8px;" />
  </div>

  <!-- 3. 새 글 알림 -->
  <div style="max-width: 220px; text-align: center; background: #f9f9f9; border-radius: 8px; padding: 16px;">
    <h3 style="margin: 12px 0 8px;">새 글 알림</h3>
    <p style="font-size: 14px; line-height: 1.4;">
      새 글이 등록되면<br/>
      실시간 이메일 알림
    </p>
    <img src="https://github.com/user-attachments/assets/f57c0382-7b5f-4726-a10b-afe018fafd24"
         alt="새 글 알림"
         style="width: 50%; border-radius: 8px; margin-top: 8px;" />
  </div>

  <!-- 4. 구독 관리 -->
  <div style="max-width: 220px; text-align: center; background: #f9f9f9; border-radius: 8px; padding: 16px;">
    <h3 style="margin: 12px 0 8px;">구독 관리</h3>
    <p style="font-size: 14px; line-height: 1.4;">
      구독 중인 RSS를<br/>
      손쉽게 관리
    </p>
    <img src="https://github.com/user-attachments/assets/f7dbaf33-dc96-4aa5-8746-9923c8889e67"
         alt="구독 관리"
         style="width: 100%; border-radius: 8px; margin-top: 8px;" />
  </div>
</div>

<br/>

## 🔍 프로젝트 정보
### 개발 기간
- 2025.02.22 ~ 2025.02.26 (5일)

### 서비스 링크
- **서비스 주소**: https://feedping.co.kr
  
- **API 서버**: https://api.feedping.co.kr

<br/>

## 🛠 기술 스택
### 💻 Language & Framework
- Java 21
- Spring Boot 3.4
- Spring Data JPA
- Spring Data Redis

### 📊 Database & Storage
- MySQL
- Redis (이메일 인증 코드 및 인증 상태 관리)

### 🏗 Infra
- AWS EC2
- AWS RDS
- Docker & Docker-Compose

### 🔧 Development Tools
- GitHub Actions (CI/CD)
- Gradle

### ⚙️ Frontend
- HTML/CSS/JavaScript
