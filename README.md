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

[발표 슬라이드]()
<br>

## <a id="1-프로젝트-개요"></a> 1. 프로젝트 개요  
### 1.1 프로젝트 소개
[프로젝트 기획서](https://docs.google.com/document/d/1WtzSRvkpIZyfA1Ly5Uefu3BVrmNXd_YseDzOPMAbrlo/edit?tab=t.0#heading=h.d5g3yog13kgn)  
**ORDER101**은 AI 기반 의사결정 자동화로 공급사–본사–점포의 발주·재고·주문·물류(SCM)를 지능화하여 재고 손실 최소화와 운영 효율 극대화하는 주문 관리 시스템입니다.  

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

<br></br>
### 1.4 주요 기능

<details>
  <summary><b>1. 주문 관리</b></summary>
  <div markdown="1">
    <ul>
      <li>주문 생성/승인/내역 조회</li>
      <li>주문 상세 페이지</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>2. 발주 관리</b></summary>
  <div markdown="1">
    <ul>
      <li>발주 생성</li>
      <li>발주 승인 및 관리</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>3. 창고 재고 관리</b></summary>
  <div markdown="1">
    <ul>
      <li>재고 현황</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>4. 상품 관리</b></summary>
  <div markdown="1">
    <ul>
      <li>상품 카탈로그</li>
      <li>상품 등록/수정/상세</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>5. 공급사 관리</b></summary>
  <div markdown="1">
    <ul>
      <li>공급사 조회</li>
      <li>공급사 상세</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>6. 정산 관리</b></summary>
  <div markdown="1">
    <ul>
      <li>정산 관리 리스트</li>
      <li>정산 리포트</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>7. 수요예측</b></summary>
  <div markdown="1">
    <ul>
      <li>AI 예측 모듈</li>
      <li> 외부요인 적용</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>8. 스마트 발주</b></summary>
  <div markdown="1">
    <ul>
      <li> AI 수요예측 기반 자동 제안</li>
      <li> 현재고 및 안전재고를 반영한 발주</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>8. 자동 발주</b></summary>
  <div markdown="1">
    <ul>
      <li>안전 재고 규칙 기반 자동 발주 제안</li>
      <li>현재 재고 및 안전재고, 리드 타임 반영한 발주주</li>
    </ul>
  </div>
</details>
<details>
  <summary><b>9. 대시보드 및 리포트</b></summary>
  <div markdown="1">
    <ul>
      <li>주문/발주 및 배송 대시보드</li>
      <li>정산 리포트</li>
    </ul>
  </div>
</details>



<br/>

<br/>

## 2. WBS
[WBS](https://docs.google.com/spreadsheets/d/1isf6GO9iqXJyhFxtxYRvIOtjqf20IP69H_oeq2SgyC8/edit?gid=509945759#gid=509945759)


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

### AI
<img src="https://img.shields.io/badge/LightGBM-024F27?style=for-the-badge&logo=lightgbm&logoColor=white"> <img src="https://img.shields.io/badge/pandas-150458?style=for-the-badge&logo=pandas&logoColor=white"> <img src="https://img.shields.io/badge/numpy-013243?style=for-the-badge&logo=numpy&logoColor=white"> <img src="https://img.shields.io/badge/scikit--learn-F7931E?style=for-the-badge&logo=scikitlearn&logoColor=white"> <img src="https://img.shields.io/badge/matplotlib-11557C?style=for-the-badge&logo=plotly&logoColor=white"> <img src="https://img.shields.io/badge/joblib-0088CC?style=for-the-badge&logo=python&logoColor=white"> 


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

<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white">
<img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
<img src="https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white">
<img src="https://img.shields.io/badge/Argo%20CD-FE6A16?style=for-the-badge&logo=argo&logoColor=white">


<br/>

## 5. 시스템 아키텍처
<details>
  <summary><b>시스템 아키텍처</b></summary>
  <div markdown="1">
    <ul>
      <img width="1177" height="1359" alt="image" src="https://github.com/user-attachments/assets/0e84db9c-21db-4a25-b530-e414f8e76226" />
      <img width="1170" height="702" alt="image" src="https://github.com/user-attachments/assets/7555252a-87c2-4bbb-9c90-d80e8d15c830" />
    </ul>
  </div>
</details>



<br/>


## 6. 데이터베이스 설계 (ERD)

[ERD CLOUD](https://www.erdcloud.com/d/Lkxma6zWujnHvtvY3)
  
<details>
  <summary><b>ERD</b></summary>
  <div markdown="1">
    <ul>
      <img width="5500" height="2412" alt="오더101 (1)" src="https://github.com/user-attachments/assets/83de89df-73fd-4137-85de-5110a53449ec" />
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



## 14. 향후 개선 계획



<br/>

## 15. 회고록

|   조원 이름	| 회고  	 |
|---	|-------|
| 조상원 | |
| 박진우 | |
| 윤석현 | |
| 이진구 | |
| 최유경 | |
