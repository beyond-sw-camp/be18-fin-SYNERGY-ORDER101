<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Noto+Sans+KR&weight=900&size=48&duration=2300&pause=900&color=BF00FF&background=00000000&center=true&vCenter=true&width=1000&lines=ORDER101;TEAM+SYNERGY" alt="ORDER101 Typing">
</p>

---

## 👥 팀원 소개

<table>
  <tr align="center">
    <td>조상원</td>
    <td>박진우</td>
    <td>윤석현</td>
    <td>이진구</td>
    <td>최유경</td>
  </tr>
  <tr align="center">
    <td><a target="_blank" href="https://github.com/sangwon5579"><img src="https://avatars.githubusercontent.com/u/81066249?v=4" width="100px"><br>@sangwon5589</a>  </td>
    <td><a target="_blank" href="https://github.com/JINWOO-0715"><img src="https://avatars.githubusercontent.com/u/55976921?v=4" width="100px"><br>@JINWOO-0715</a></td>
    <td><a target="_blank" href="https://github.com/xxiuan"><img src="https://avatars.githubusercontent.com/u/156274066?v=4" width="100px"><br>@xxiuan</a> </td>
    <td><a target="_blank" href="https://github.com/LeeJingu01"><img src="https://avatars.githubusercontent.com/u/174857452?v=4" width="100px"><br>@LeeJingu01</a> </td>
    <td><a target="_blank" href="https://github.com/kyounggg"><img src="https://avatars.githubusercontent.com/u/114654921?v=4" width="100px"><br>@kyounggg</a>  </td>
  </tr>
</table>


## 📚 목차

0. [발표 슬라이드](#0-발표-슬라이드)  
1. [프로젝트 개요](#1-프로젝트-개요)
2. [WBS](#2-WBS)
3. [요구사항 명세서](#3-요구사항-명세서)  
4. [기술 스택](#4-기술-스택)  
5. [시스템 아키텍처](#5-시스템-아키텍처)  
6. [데이터베이스 설계 (ERD)](#6-데이터베이스-설계-erd)  
7. [테이블 명세서](#7-테이블-명세서)  
8. [API 명세서](#8-api-명세서)
9. .[화면 기능 설계서](#9-화면-기능-설계서)
10. [백엔드 테스트 결과서](#10-백엔드-테스트-결과서)
11. .[프론트엔드 테스트 결과서](#11-프론트엔드-테스트-결과서)
12. .[CICD](#12-CICD)
13. .[트러블 슈팅](#13-트러블-슈팅)
14. [향후 개선 계획](#14-향후-개선-계획)  
15. [회고록](#15-회고록)




<br/>

## <a id="0-발표-슬라이드"></a> 0. 발표 슬라이드

[발표 슬라이드](https://www.canva.com/design/DAG6-Nh9Ixs/bA0Cm1fcNDg14sJtg0QeTw/edit)
<br>

## <a id="1-프로젝트-개요"></a> 1. 프로젝트 개요  
### 1.1 프로젝트 소개
<img width="1280" height="543" alt="11111111-Photoroom" src="https://github.com/user-attachments/assets/d0b852ba-c234-4c71-9101-0bfe0dbdcd4a" />
**ORDER101**은 AI 기반 의사결정 자동화로 공급사–본사–점포의 발주·재고·주문·물류(SCM)를 지능화하여 재고 손실 최소화와 운영 효율 극대화하는 주문 관리 시스템입니다.  

[프로젝트 기획서](https://docs.google.com/document/d/1WtzSRvkpIZyfA1Ly5Uefu3BVrmNXd_YseDzOPMAbrlo/edit?tab=t.0#heading=h.d5g3yog13kgn)  

<br></br>
### 1.2 프로젝트 배경
- 재고관리 : 수동 및 경험 기반의 발주로 과잉 재고 및 품절 발생 빈번.   
- 운영 효율 : 발주 프로세스가 느리고, 직영점별 관리가 비효율적이며 일관성이 부족함.  
- 경쟁 우위 : 경쟁사들은 이미 데이터 기반 혁신을 가속화하는 추세. 데이터 활용 역량 격차 심화 시 장기적인 리테일 경쟁력 약화 우려.  


<img width="2070" height="584" alt="image" src="https://github.com/user-attachments/assets/8584b2b4-d285-43de-b129-f8dc7a47d311" />
“stockouts (품절)이 전세계 리테일러에 연간 약 1조 달러(약 1,000 조 원) 이상 손실을 안겨주고 있다”  <br>
-> 수요 예측 실패, 재고관리 부정확성, 발주 프로세스 지연 등이 주요 원인  <br>  
<a href="https://www.mirakl.com/blog/out-of-stocks-ecommerce-inventory-management-problem?">출처</a>
<br></br>
<img width="1584" height="463" alt="image" src="https://github.com/user-attachments/assets/9cc90bbf-9f30-4d7c-8d15-c6999f3753c6" />
매장의 과거 판매·발주·재고 데이터 + 외부 변수를 사용하여 최적 발주량을 계산한다.<br>  
<a href="https://www.etnews.com/20250331000062?">출처</a>
<br></br>
<img width="1621" height="406" alt="image" src="https://github.com/user-attachments/assets/d9c56de2-f8d7-4f06-9b1d-62dd6eb8c10c" />
AI가 팔릴 상품 수량 예측하고, 발주까지 자동으로 넣는다.<br>  
<a href="https://biz.chosun.com/site/data/html_dir/2020/04/06/2020040602892.html?">출처</a>


<br></br>
### 1.3 기존 서비스와의 차별점
#### 수요 예측
과거 발주·판매·재고 데이터를 기반으로 상품별·지점별 수요를 예측해 품절과 과잉재고를 동시에 줄임.
LightGBM 모델 기반의 시계열 분석으로 계절성, 요일 패턴, 프로모션 효과를 반영.
정확한 수요량을 사전 계산해 재고 회전율 향상 및 낭비 최소화 달성.

#### 자동 발주
예측 결과와 안전재고 규칙을 기반으로 점포별 최적 발주량을 자동 계산 및 제안.
제안값의 근거(안전재고, 최소/최대 발주수량)를 함께 표시해 신뢰성 있는 의사결정 지원.
현재고, 리드타임, 안전 재고를 반영하여 모니터링하며 필요한 순간 바로제안.

#### 스마트 발주
수요 예측 데이터에 근거하여 최적 발주량을 자동 계산 및 제안.
일주일에 한번 AI가 발주의 초안을 근거와 함께 작성 및 제안.
신뢰성을 제공하는 동시에 편의성을 제공.

#### 채팅
가맹점이 발주 수정 및 삭제를 원하는 경우 직접 본사에 문의하는 프로세스를 더욱 편리하게 제공.  
가맹점과 담당 본사 직원간의 채팅 기능 제공.  
서비스 내에서 더욱 편리한 양방향 소통 제공.  


<br></br>
### 1.4 주요 기능
<details>
  <summary><b>1. 가맹점 주문 관리</b></summary>
  <ul>
    <li>주문 생성 / 승인 / 내역 조회</li>
      <img 
        src="https://github.com/user-attachments/assets/ee2d1c00-34da-4453-a9aa-e0aa24434b5f"
        width="700"
      />
      <img 
        src="https://github.com/user-attachments/assets/ecd07829-de6d-4c2f-86f0-e7be5fc56f6c"
        width="700"
      />
      <img 
        src="https://github.com/user-attachments/assets/a57eda77-23f7-473a-89ae-2dd457e8f957"
        width="700"
      />
    <li>배송 상세 조회</li>
      <img 
        src="https://github.com/user-attachments/assets/6872e7e9-e181-4737-857c-ed3c92866e6b"
        width="700"
      />
    <li>주문 대시보드</li>
      <img 
        src="https://github.com/user-attachments/assets/e59ac82b-ea97-49c8-8fab-63a9a5b37a27"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>2. 본사 발주 관리</b></summary>
  <ul>
    <li>일반 발주 생성</li>
      <img 
        src="https://github.com/user-attachments/assets/93f928c0-d857-4015-a675-c4caf9d116c7"
        width="700"
      />
    <li>발주 승인 / 내역 조회</li>
      <img 
        src="https://github.com/user-attachments/assets/66a564a9-d169-425e-8b8a-98a0dbcb9363"
        width="700"
      />
      <img 
        src="https://github.com/user-attachments/assets/331dc52b-09ac-46a2-b948-c8e944a8772a"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>3. 창고 관리</b></summary>
  <ul>
    <li>본사/가맹점 창고 재고</li>
      <img 
        src="https://github.com/user-attachments/assets/f8574a09-87fd-401e-8e59-8857314bebbe"
        width="700"
      />
      <img 
          src="https://github.com/user-attachments/assets/61ac7649-9172-4741-9a48-9429956bcf78"
          width="700"
        />
    <li>본사 창고 입고/출고</li>
      <img 
        src="https://github.com/user-attachments/assets/b31772eb-cde8-4309-8cf5-7fd0c0201ff4"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>4. 상품 관리</b></summary>
  <ul>
    <li>상품 목록 / 상세</li>
      <img 
        src="https://github.com/user-attachments/assets/8cf9e2be-a72c-4412-becb-bfc57b33e1b0"
        width="700"
      />
    <li>상품 등록</li>
      <img 
        src="https://github.com/user-attachments/assets/7425276b-b5ea-49bc-9805-8cb03e7217c5"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>5. 공급사 관리</b></summary>
  <ul>
    <li>공급사 조회/상세</li>
      <img 
        src="https://github.com/user-attachments/assets/d303600b-e55b-4d28-a6e8-796fe274bd5c"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>6. 정산 관리</b></summary>
  <ul>
    <li>정산 목록</li>
      <img 
        src="https://github.com/user-attachments/assets/8a83cb16-6ec8-4054-a1bb-4358cce16fa5"
        width="700"
      />
    <li>정산 리포트</li>
      <img 
        src="https://github.com/user-attachments/assets/2ec9cea5-a967-460a-b1e2-4d96abfd1014"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>7. 수요 예측</b></summary>
  <ul>
    <li>수요 예측</li>
      <img 
        src="https://github.com/user-attachments/assets/0bf23a70-fd21-4719-866e-35e068bf512d"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>8. 스마트 발주</b></summary>
  <ul>
    <li>스마트 발주 초안 생성/수정/제출</li>
      <img 
        src="https://github.com/user-attachments/assets/465ca2ac-d449-4c8d-9ca5-0408f9d962fa"
        width="700"
      />
    <li>스마트 발주 승인/반려</li>
      <img 
        src="https://github.com/user-attachments/assets/81971e32-0d3e-46f5-a505-25fe02a446ef"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>9. 자동 발주</b></summary>
  <ul>
    <li>자동 발주 초안 생성/수정/제출</li>
      <img 
        src="https://github.com/user-attachments/assets/dc1a60e2-252c-4c96-89d8-d4663d37e0e8"
        width="700"
      />
    <li>자동 발주 승인/반려</li>
      <img 
        src="https://github.com/user-attachments/assets/3a0e49fc-2bdd-4f2c-b067-d9c3334a5830"
        width="700"
      />
  </ul>
</details>
<details>
  <summary><b>10. 채팅 & 챗봇</b></summary>
  <ul>
    <li>본사/가맹점 채팅</li>
      <img 
        src="https://github.com/user-attachments/assets/1a84f07f-48fd-469b-8db3-ce64c426226e"
        width="700"
      />
    <li>본사 챗봇</li>
      <img 
        src="https://github.com/user-attachments/assets/93d49199-f51b-4a66-abba-26675771c43e"
        width="700"
      />
  </ul>
</details>



<br/>

<br/>

## 2. WBS
[WBS](https://docs.google.com/spreadsheets/d/1isf6GO9iqXJyhFxtxYRvIOtjqf20IP69H_oeq2SgyC8/edit?gid=509945759#gid=509945759)
<details>
  <summary><b>WBS</b></summary>
  <div markdown="2">
    <ul>
    <img width="1463" height="1217" alt="image" src="https://github.com/user-attachments/assets/01468760-3a6d-445b-87d1-e661171d42eb" />
    <img width="1463" height="953" alt="image" src="https://github.com/user-attachments/assets/39164193-4435-4fd7-bceb-c03f2def50d8" />
    </ul>
  </div>
</details>



## 3. 요구사항 명세서

### 기능 요구사항

#### 3.1 요약

| 구분 | 주요 기능 | 핵심 요약 설명 |
|----------------|--------|--------|
| 사용자 관리 | HQ_ADMIN이 HQ_STAFF 및 STORE_OWNER 계정 발급 | 역할 기반 접근(RBAC/ABAC) 적용, MFA 및 세션 정책 포함 |
| 상품 관리 | SKU 단위 상품 생성·수정·비활성 | 거래 연계 데이터 보호, 카테고리별 검색 및 상태관리 |
| 발주 관리 | 일반 발주 | 일반 직원이 발주 생성,공급사 승인/반려 및 출고 연동 |
| 발주 관리 | 스마트 발주 | AI 예측을 반영한 자동 생성, 일반 직원의 수정, 공급사 승인/반려 및 출고 연동 |
| 발주 관리 | 자동 발주 | 안전재고를 반영한 자동 생성, 일반 직원의 수정, 공급사 승인/반려 및 출고 연동 |
| 주문 관리 | 가맹점 주문·승인·정산 일원화 | 주문 생성, 검수, 정산의 과정 제공 |
| 재고/창고 관리 | 입·출고 및 안전재고 규칙 | 창고별 입출고 이력 관리, 안전재고 임계값 이하 시 경고 표시 |
| 수요 예측 | AI 수요 예측 | 과거 판매·발주·재고 데이터 및 외부요인을 학습하여 SKU×지점별 단기·중기 수요를 예측. |
| 정산 관리 | 자동 금액 산정 및 리포트 | AR(가맹점청구)/AP(공급사지급) 구분 정산, 기간별 대시보드 제공 |


#### 3.2 전문
[요구사항 명세서](https://docs.google.com/spreadsheets/d/1vx25t4TzY9Tyu7JUvGRLR6c3iasbg0lG-xEiHt3Kacs/edit?gid=0#gid=0)  
<details>
  <summary><b>요구사항 명세서</b></summary>
  <div markdown="2">
    <ul>
    <img width="1517" height="926" alt="image" src="https://github.com/user-attachments/assets/bf0ae578-1814-4c97-8193-751e7e0f8e64" />
    <img width="1516" height="1097" alt="image" src="https://github.com/user-attachments/assets/e1cd39e0-af66-403a-a15e-59fc271860ec" />
    <img width="1519" height="427" alt="image" src="https://github.com/user-attachments/assets/f957407e-5afd-439f-aae3-db753a8dd736" />
    </ul>
  </div>
</details>


</div>
</details>

<br/>


## 4. 기술 스택
### BACKEND


![java](https://github.com/user-attachments/assets/a9cd03e7-07d6-477e-b3dd-32e7a6ae1e08)
![jpa](https://github.com/user-attachments/assets/dd9fdaec-6850-4401-9c67-af2da34ddf5d) 
<img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
![jwt](https://github.com/user-attachments/assets/83bddf8b-d556-4e60-8391-2074704103c4)
<img src="https://img.shields.io/badge/SpringBoot-10B146?style=for-the-badge&logo=SpringBoot&logoColor=white">
<img src="https://img.shields.io/badge/SpringSecurity-3B66BC?style=for-the-badge&logo=SpringSecurity&logoColor=white">
<img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white">
<img src="https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white">
<img src="https://img.shields.io/badge/Poetry-60A5FA?style=for-the-badge&logo=poetry&logoColor=white">
<img src="https://img.shields.io/badge/Uvicorn-4051B5?style=for-the-badge&logo=python&logoColor=white">

### AI
<img src="https://img.shields.io/badge/LightGBM-024F27?style=for-the-badge&logo=lightgbm&logoColor=white"> <img src="https://img.shields.io/badge/pandas-150458?style=for-the-badge&logo=pandas&logoColor=white"> <img src="https://img.shields.io/badge/numpy-013243?style=for-the-badge&logo=numpy&logoColor=white"> <img src="https://img.shields.io/badge/scikit--learn-F7931E?style=for-the-badge&logo=scikitlearn&logoColor=white"> <img src="https://img.shields.io/badge/matplotlib-11557C?style=for-the-badge&logo=plotly&logoColor=white"> <img src="https://img.shields.io/badge/joblib-0088CC?style=for-the-badge&logo=python&logoColor=white"> 
<img src="https://img.shields.io/badge/CatBoost-FF6F00?style=for-the-badge&logo=catboost&logoColor=white">
<img src="https://img.shields.io/badge/multilingual--e5--base-4B32C3?style=for-the-badge&logo=huggingface&logoColor=white">
<img src="https://img.shields.io/badge/ChatGPT-00A67E?style=for-the-badge&logo=openai&logoColor=white">

### FRONTEND
  
<img src="https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white"> <img src="https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white">
<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black">
<img src="https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white">
<img src="https://img.shields.io/badge/Pinia-F8E162?style=for-the-badge&logo=pinia&logoColor=black">
<img src="https://img.shields.io/badge/Bootstrap-7952B3?style=for-the-badge&logo=bootstrap&logoColor=white">




### DATABASE


![redis](https://github.com/user-attachments/assets/df929d81-ce2f-4853-97fd-cdf7bf45907e) ![mariadb](https://github.com/user-attachments/assets/19a0ad09-804d-4303-80bd-32cafdae0e6f)




### API
<img src="https://img.shields.io/badge/apidog-FE4F19?style=for-the-badge&logoColor=white">




### IDE

![intellij](https://github.com/user-attachments/assets/25d426ed-e30e-4619-9968-11375adba8b9)
<img src="https://img.shields.io/badge/VS%20Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white">
<img src="https://img.shields.io/badge/jupyter-F37626?style=for-the-badge&logo=jupyter&logoColor=white">

### DEVOPS

<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"> <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20CloudFront-8C4FFF?style=for-the-badge&logo=amazoncloudfront&logoColor=white"> <img src="https://img.shields.io/badge/AWS%20CodeDeploy-527FFF?style=for-the-badge&logo=awsdeploy&logoColor=white"> <img src="https://img.shields.io/badge/AWS%20CodePipeline-527FFF?style=for-the-badge&logo=awscodepipeline&logoColor=white">






<br/>

## 5. 시스템 아키텍처
<details>
  <summary><b>시스템 아키텍처</b></summary>
  <div markdown="1">
    <ul>
      <img width="9992" height="8259" alt="order101_sy" src="https://github.com/user-attachments/assets/92275fa6-2d8d-42b2-8b1b-01d6e687ca4f" />
    </ul>
  </div>
</details>




<br/>


## 6. 데이터베이스 설계 (ERD)

[ERD CLOUD](https://www.erdcloud.com/d/BNoDxLbwss3ZbKg8x)
  
<details>

  <summary><b>ERD</b></summary>
  <div markdown="1">
    <ul>
      <img width="5470" height="2332" alt="오더101 (2)" src="https://github.com/user-attachments/assets/2d019e1e-78d5-4827-858c-c0c8c0ad4a77" />
    </ul>
  </div>
</details>

<br>


## 7. 테이블 명세서

[테이블 명세서](https://docs.google.com/spreadsheets/d/1L8VvISg4ghGQ_SOjZdJVomAtUtR1-A0axD3hO7-VWj4/edit?gid=1345457380#gid=1345457380)


<br>

## 8. API 명세서

[API 명세서](https://4ktgjt483l.apidog.io)

<br/>

## 9. 화면 기능 설계서 

[화면 기능 설계서](https://www.figma.com/design/8mX2qcFswjZHQQIoh3RJ8x/ORDER101?node-id=0-1&p=f&t=gZyv3W4HDc5EMKRX-0)

<br/>

## 10. 백엔드 테스트 결과서
<details>
  <summary><b>단위 테스트</b></summary>
  <div markdown="1">
    <ul>
      <img width="1262" height="1160" alt="image" src="https://github.com/user-attachments/assets/f783ba29-076c-4d51-a291-07f982aed506" />
      <img width="1305" height="1430" alt="image" src="https://github.com/user-attachments/assets/62aaa1e2-1388-41a2-bba8-a0c1a91b033d" />
    </ul>
  </div>
</details>

<details>
  <summary><b>통합 테스트</b></summary>
  <div>
    <ul>
      <li>
        <a href="https://docs.google.com/spreadsheets/d/1zyB0iD03mgLm-BNBLT5-Q5ppBiXPDqPmvfoJyA0rz2U/edit?gid=0#gid=0" target="_blank">
          통합 테스트 결과서
        </a>
        <br>
      </li>
      <img width="722" height="1137" alt="image" src="https://github.com/user-attachments/assets/5bfd7014-fbe7-4d22-b06a-7c6a07163056" />
    </ul>
  </div>
</details>




## 11. 프론트엔드 테스트 결과서

[UI/UX 테스트 결과서](https://www.notion.so/playdatacademy/UI-UX-2bfd943bcac2801390ecde7eb9f1fb4a?source=copy_link)

## 12. CICD
[CICD 계획서](https://docs.google.com/document/d/1-79JuduMko7PyVVVVHkhjOkVK2aWvqL65Bw6ZQA6s5g/edit?usp=sharing)


## 13. 트러블 슈팅
<details>
  <summary><b> <h3>1. 조회 성능 개선  </b></summary>
  <div markdown="1">
    <ul>
      <li>
1.1. 문제 상황     <br>
“수요 예측 보고서“ 호출 시 지연 발생    <br>
반복 조회 시에도 항상 DB에서 전체 데이터를 Selcet      <br> 
대시보드 호출이 화면 전환 때마다 느림    <br>   </li>
  <li>
1.2 원인       <br>
해당 서비스 매서드들은 매번 DB에 접근하여 대량의 데이터를 그룹핑 및 집계/정렬하는 연산을 수행       <br>
캐시가 적용되지 않아 매 호출마다 동일 연산 발생 → 응답 시간이 누적   <br>    </li>
  <li>
1.3. 해결     <br>
Spring Cache 도입    <br>  </li>
  <li>
1.4. 추가 조치     <br>
캐싱 시 Lazy Loading 직렬화 오류 해결       <br>
Redis 저장 시 JPA Lazy Proxy가 직렬화 오류를 발생시키므로 서비스 단계에서 필요한 필드를 미리 초기화     <br> </li>
    </ul>
  </div>
</details>
<details>
  <summary><b> <h3>2.  알림 과다 요청   </b></summary>
  <div markdown="1">
    <ul>
      <li>   
              2.1. 문제 상황 & 원인  <br> 
에러 발생 시 무제한 3초 재연결 시도  <br> 
동일 사용자에 대해 SSE 연결과 emitter가 누적  <br> 
error 이벤트 중복 처리로 재연결 타이머가 여러 개 생성  <br> 
 → /notifications SSE 요청 폭주 → CloudFront LimitExceeded 503 → 프론트 전체 장애    <br> </li>
         <li>
2.2. 해결  <br> 
사용자당 서버 emitter 1개, 클라이언트 SSE 연결 1개만 유지  <br> 
재연결 타이머 단일화 + 지수 백오프로 트래픽 스파이크 방지  <br> 
토큰 오류와 일반 네트워크 오류를 분리 처리  <br> 
ALB/CloudFront 타임아웃 조정으로 장시간 SSE 연결 안정화     <br> </li>
    </ul>
  </div>
</details>



## 14. 향후 개선 계획  
### 1. 조회 성능 개선  
전체 목록 조회시 느려지는 현상 개선  
더욱 방대한 데이터 핸들링  

### 2. 성능 고도화
랭체인을 접목해 내부 DB를 학습한 기업 전용 챗봇으로 고도화  
동시성 제어 등 성능 고도화  

### 3. 프로세스 전체 구현  
주요 기능에 집중하고자 간소화했던 기능(ex.배송처리 등)들까지 완벽하게 구현  






<br/>

## 15. 회고록

|   조원 이름	| 회고  	 |
|-----	|-------|
| 조상원 | 웹을 기획부터 실제 배포까지 전체 과정을 이끌며 전체 프로세스를 체화할 수 있는 좋은 기회였다. 특히 Spring 벡엔드와 Vue.js 프론트엔드에서 그치는게 아닌 AI를 집중적으로 다룰 수 있어 시야를 넓힐 수 있었던 프로젝트였다. 이후 AWS를 사용해 실제 배포를 해보며 우리의 도메인에 접속할 수 있는게 새로운 느낌이었다. 단순 기능 구현이 아닌 하나하나 직접 고민해보며 적용해본 경험들이 이후의 개발에 있어서 큰 도움이 될 수 있을거 같다. |
| 박진우 | aws cicd를 해보는 좋은 기회가 됐다. 에러가 많이 났는데 향후 다른 프로젝트를 진행할때 기준점을 제시할 수 있는 경험이엿다. 근거를 제시하는 연습을 충분히 할 수 있는 성과 있었다.  |
| 윤석현 | 이번 프로젝트는 처음으로 B2B 시스템을 설계부터 개발까지 해보는 경험이었다. 처음에는 막막했지만 요구사항을 분석하고 설계하는 과정에서 많은 것을 배울 수 있었다. 로그인 프로세스와 권한 관리를 구현하면서 보안에 대한 중요성을 다시 한 번 깨달았다. 이번 프로젝트를 통해 얻은 경험을 바탕으로 앞으로 더 나은 시스템을 개발할 수 있을 것 같다. |
| 이진구 | RestFul API, SSE, WebSocket등을 사용하면서 다방면으로 기술 스택을 넓힌 것 같으며, 로컬이 아닌 실제로 AWS를 사용하여 사용자들이 사용할 수 있는 서비스를 배포하면서 실무와 비슷한 경험을 한 것 같아 성장한 것 같은 느낌이 든다. 또한 B2B에 대해 도메인을 어느 정도 알게 되어 만족스러운 느낌이 들었다. |
| 최유경 | 기능 구현에서 끝나는것이 아니라 배포까지 하는 프로젝트는 처음이라 좋은 경험이었다. 기획부터 배포까지 전체 프로세스를 해보며 기획 단계가 중요하다는 것을 다시 한 번 깨달을 수 있었다.  |
