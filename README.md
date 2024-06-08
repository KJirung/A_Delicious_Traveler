# :fork_and_knife: 맛있는 여행자 :fork_and_knife:
<br/>

## :pushpin: 개요
- 맛있는 여행자란?
- 프로젝트 선정 배경
- 프로그램 흐름도
- 사용된 기술 스택
- 시연 영상

---
<br/>

## :fork_and_knife: 맛있는 여행자란?
- 외국인 관광객을 위한 어플로, 먼저 사용자가 촬영한 음식 사진에서 음식을 탐지합니다. 이후 탐지된 음식들 중에서 사용자가 원하는 음식을 터치하면 해당하는 음식을 판별한 후 음식에 관한 간단한 정보(레시피, 유래 등)를 제공합니다. 

---
<br/>

## ✔️프로젝트 선정 배경
- 최근 K팝, K드라마 등 일명 'K콘텐츠'의 전세계적 유행으로 한국을 방문하는 관광객들이 많아지면서 인공지능을 활용해 관광객들에게 도움이 될 수 있는 앱을 제작하고자 이 프로젝트 주제를 선정하게 되었습니다.

---
<br/>

## ✔️Flow Chart
- 프로그램의 전체적인 흐름도
- 앱 내 카메라 및 저장소 액세스 접근 기능 구현
- 이미지 탐지는 Yolov5m모델을 사용
- 이미지 분류는 InceptionV3 모델을 사용
- 2개의 딥러닝 모델을 앱 내에서 처리하는 온디바이스로 구현
- 음식에 관한 정보는 RAG를 통해 LLM과 외부 데이터셋을 참조하여 제공

![image](https://github.com/KJirung/A_Delicious_Traveler/assets/142071404/ee2c16d1-1203-4f09-891d-7e13ff798b91)

---
<br/>

## :shopping_cart: 사용된 기술 스택
- 프로그래밍 언어
   - Kotlin, Python
- 프레임워크
   - Pytorch, Tensorflow, Keras 
- 개발 도구 및 환경
   - Android Studio, Visual Studio Code, Jupyter Notebook

---
<br/>

## :video_camera:시연 영상


![시연영상](https://github.com/KJirung/A_Delicious_Traveler/assets/142071404/e9935591-6bce-4cc5-ad6e-0ae44a678ae0)

