import torch
from PIL import Image, ImageDraw
import torchvision.transforms as transforms

# YOLOv5 라이브러리 임포트
from models.experimental import attempt_load
from utils.general import non_max_suppression

def visualize_boxes(image_path, model_path, conf_thres=0.4, iou_thres=0.5):
    # 모델 불러오기
    model = attempt_load(model_path)

    # 이미지 로드
    img = Image.open(image_path).convert('RGB')

    # 추론할 이미지 크기 저장
    img_width, img_height = img.size

    # 이미지 크기를 224x224로 조정
    transform = transforms.Compose([
        transforms.Resize((224, 224)),
        transforms.ToTensor(),
    ])
    img_tensor = transform(img).unsqueeze(0)

    # 추론
    pred = model(img_tensor)[0]

    # 탐지된 객체에 대해 NMS 수행
    pred = non_max_suppression(pred, conf_thres, iou_thres)

    # 바운딩 박스 시각화
    if pred[0] is not None:
        draw = ImageDraw.Draw(img)
        for det in pred[0]:
            box = det[:4].int()

            # 원본 이미지에 맞게 바운딩 박스 크기 조정
            x_min = int(box[0] * img_width / 224)
            y_min = int(box[1] * img_height / 224)
            x_max = int(box[2] * img_width / 224)
            y_max = int(box[3] * img_height / 224)

            # 선의 두께 설정
            line_width = 4

            draw.rectangle([(x_min, y_min), (x_max, y_max)], outline="red", width=line_width)

    # 시각화된 이미지 보기
    img.show()

# 이미지 경로와 모델 가중치 파일 경로
image_path = 'test_images/plate3.jpg'
model_path = 'runs/train/exp15/weights/best.pt'  # 학습된 모델 가중치 파일

# 객체 탐지 및 시각화 수행
visualize_boxes(image_path, model_path)
