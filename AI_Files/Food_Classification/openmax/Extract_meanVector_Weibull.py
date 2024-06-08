import os
from keras.preprocessing.image import ImageDataGenerator
import matplotlib.pyplot as plt
from keras.models import  Model, load_model
from scipy.stats import weibull_min
import tensorflow as tf
import numpy as np

import json

base_dir = 'to_extract_mean_vector_data'



test_datagen = ImageDataGenerator(rescale= 1./255)

test_generator = test_datagen.flow_from_directory(
    base_dir,
    target_size = (299, 299),
    batch_size = 32,
    seed = 42
)

def custom_loss(y_true, y_pred):
    return tf.nn.softmax_cross_entropy_with_logits(labels=y_true, logits=y_pred)


model = load_model('best_model_0.8705.h5', custom_objects={'custom_loss': custom_loss})


feature_model = Model(inputs=model.input, outputs=model.output)



categories = len(test_generator.class_indices)
mean_vectors = []
weibull_models = []

for category in range(categories):
    category_features = []
    test_generator.reset()
    
    # 모든 배치를 순회하며 데이터 수집
    for i in range(len(test_generator)):
        x_batch, y_batch = next(test_generator)
        batch_features = feature_model.predict(x_batch)
        filtered_features = batch_features[np.where(y_batch[:, category] == 1)[0]]
        category_features.extend(filtered_features)
    
    
    category_features = np.array(category_features)
    if category_features.ndim == 1:  # category_features가 1차원 배열인 경우
        category_features = np.expand_dims(category_features, axis=0)
    
    mean_vector = np.mean(category_features, axis=0)
    mean_vectors.append(mean_vector)
    
    distances = np.linalg.norm(category_features - mean_vector, axis=1)
    weibull_model = weibull_min.fit(distances, floc=0)
    weibull_models.append(weibull_model)



mean_vectors = [mean_vector.tolist() for mean_vector in mean_vectors]
weibull_models = [
    {'shape': model[0], 'loc': model[1], 'scale': model[2]} for model in weibull_models
]

openmax_params = {
    "mean_vectors": mean_vectors,
    "weibull_models": weibull_models
}


with open('../../../openmax_params.json', 'w') as file:
    json.dump(openmax_params, file)


