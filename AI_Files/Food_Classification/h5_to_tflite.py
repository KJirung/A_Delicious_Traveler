import tensorflow as tf

# Keras 모델 불러오기
best_model = tf.keras.models.load_model('pretrained_models/InceptionV3/h5/best_model.h5')

# tflite 변환
converter = tf.lite.TFLiteConverter.from_keras_model(best_model)
tflite_model = converter.convert()

# tflite 모델 파일로 저장
with open('pretrained_models/InceptionV3/tflite/best_model.tflite', 'wb') as f:
    f.write(tflite_model)


