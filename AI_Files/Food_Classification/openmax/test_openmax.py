import json
import numpy as np
from scipy.stats import weibull_min
from keras.models import load_model
from keras.preprocessing.image import ImageDataGenerator
import tensorflow as tf
import os

base_dir = 'final_food'
test_dir = os.path.join(base_dir, 'test')

test_datagen = ImageDataGenerator(rescale= 1./255)

test_generator = test_datagen.flow_from_directory(
    test_dir,
    target_size = (299, 299),
    batch_size = 32,
    seed = 42
)

class_labels = list(test_generator.class_indices.keys())

def custom_loss(y_true, y_pred):
    return tf.nn.softmax_cross_entropy_with_logits(labels=y_true, logits=y_pred)



# Custom loss를 포함한 모델 로드
feature_model = load_model("../pretrained_models/InceptionV3/h5/best_model.h5", custom_objects={"custom_loss": custom_loss})


# openmax 점수를 계산하는 함수
def compute_openmax_score(feature_vector, mean_vectors, weibull_models):
    openmax_scores = []
    for mean_vector, weibull_model in zip(mean_vectors, weibull_models):
        # 유클리드 거리 계산
        distance = np.linalg.norm(feature_vector - mean_vector)
        
        # Weibull 누적분포 함수 계산
        weibull_probability = weibull_min.cdf(distance, weibull_model['shape'], loc=weibull_model['loc'], scale=weibull_model['scale'])
        
        # OpenMAX 점수 계산
        openmax_score = 1 - weibull_probability
        openmax_scores.append(openmax_score)
    
    return openmax_scores




# 미리 구한 Mean Vector 값과 Weibull 분포 값 로드
with open('openmax_params_tail_10.json', 'r') as file:
    openmax_params = json.load(file)

mean_vectors = openmax_params['mean_vectors']
weibull_models = openmax_params['weibull_models']

# 경계값
others_threshold = 0.5
correct_predictions = 0
total_predictions = 0

# Test 데이터에 대한 정확도 측정(Others 클래스 존재 X)
for i in range(len(test_generator)):
    images, labels = next(test_generator)
    for j in range(len(images)):
        test_image = images[j]
        true_label = labels[j]
        
        # 차원 맞추기
        test_image_expanded = np.expand_dims(test_image, axis=0)
        feature_vector = feature_model.predict(test_image_expanded)
        
        # openmax score 계산
        openmax_scores = compute_openmax_score(feature_vector[0], mean_vectors, weibull_models)
        
        # 가장 큰 openmax score 추출
        max_openmax_score = max(openmax_scores)
        
        # Others가 아닌 경우
        if max_openmax_score >= others_threshold:
            predicted_class_index = np.argmax(max_openmax_score)
            predicted_class = class_labels[predicted_class_index]
        
            true_class_index = np.argmax(true_label)
            true_class = true_class_index
            
            if predicted_class_index == true_class_index:
                correct_predictions += 1

        total_predictions += 1


# Openmax 적용 후 테스트 데이터에 대한 정확도 계산
accuracy = correct_predictions / total_predictions
print(f"Accuracy: {accuracy:.2f}")



