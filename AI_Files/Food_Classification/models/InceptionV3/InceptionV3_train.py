import os
from keras.preprocessing.image import ImageDataGenerator
from keras.applications.inception_v3 import InceptionV3
import matplotlib.pyplot as plt
from keras.callbacks import EarlyStopping, ModelCheckpoint, LearningRateScheduler
from keras.models import Model, load_model
from keras.layers import Dense, Dropout, Flatten, AveragePooling2D
from keras.optimizers import SGD
from keras.regularizers import l2
import tensorflow as tf


# 데이터셋 경로
base_dir = '../../final_food'

# train, valid, test 경로
train_dir = os.path.join(base_dir, 'train')
val_dir = os.path.join(base_dir, 'validation')
test_dir = os.path.join(base_dir, 'test')


# 이미지 제너레이터
train_datagen = ImageDataGenerator(
    rescale = 1./255,
    rotation_range=20,
    width_shift_range=0.2,
    height_shift_range=0.2,
    shear_range=0.2,
    zoom_range=0.2,
    horizontal_flip=True,
    fill_mode='nearest'
)

# 검증/테스트 데이터는 증강X
val_datagen = ImageDataGenerator(rescale = 1./255)
test_datagen = ImageDataGenerator(rescale= 1./255)


# 이미지 불러오기
train_generator = train_datagen.flow_from_directory(
    train_dir,
    target_size = (299, 299),
    batch_size = 32,
    seed = 42
)

val_generator = val_datagen.flow_from_directory(
    val_dir,
    target_size = (299, 299),
    batch_size = 32,
    shuffle=True,
    seed = 42
)

test_generator = test_datagen.flow_from_directory(
    test_dir,
    target_size = (299, 299),
    batch_size = 32,
    seed = 42
)


# imagenet 가중치 가져오기
base_model = InceptionV3(weights='imagenet', include_top=False, input_shape=(299, 299, 3))


# 레이어 추가
x = base_model.output
x = AveragePooling2D(pool_size=(8, 8))(x)
x = Dropout(.4)(x)
x = Flatten()(x)
x = Dropout(.4)(x)
predictions = Dense(150, kernel_initializer='glorot_uniform', 
                    kernel_regularizer=l2(.0005))(x)

model = Model(inputs=base_model.input, outputs=predictions)


# Early stopping 설정
early_stopping = EarlyStopping(monitor='val_accuracy', patience=7, verbose=1, restore_best_weights=True)


# ModelCheckpoint 설정
checkpoint_filepath = '../pretrained_models/InceptionV3/h5/best_model.h5'
checkpoint = ModelCheckpoint(checkpoint_filepath, monitor='val_accuracy', save_best_only=True, verbose=1)


# 옵티마이저 설정
opt = SGD(lr=.01, momentum=.9)

# 커스텀 손실함수
def custom_loss(y_true, y_pred):
    return tf.nn.softmax_cross_entropy_with_logits(labels=y_true, logits=y_pred)

# 모델 컴파일
model.compile(optimizer=opt, loss=custom_loss, metrics=['accuracy'])


# 스케쥴러 설정
def schedule(epoch):
    if epoch < 15:
        return .01
    elif epoch < 28:
        return .002
    else:
        return .0004

lr_scheduler = LearningRateScheduler(schedule)


# 모델 학습
history = model.fit(train_generator, epochs=30, validation_data=val_generator, callbacks=[early_stopping, checkpoint, lr_scheduler])


# -------------------------------------------------------------------------------------------------------------------------------------------

# 그래프 시각화
fig, axs = plt.subplots(2, 1, figsize=(10, 8))

# 손실 그래프
axs[0].plot(range(1, len(history.history["loss"]) + 1), history.history["loss"], label='Train loss', color='red')
axs[0].plot(range(1, len(history.history["val_loss"]) + 1), history.history["val_loss"], label='Validation loss', color='blue')
axs[0].set_title("Training and Validation loss")
axs[0].set_ylabel("loss")
axs[0].set_xlabel("epoch")
axs[0].legend()

# 정확도 그래프
axs[1].plot(range(1, len(history.history["accuracy"]) + 1), history.history["accuracy"], label='Train accuracy', color='red')
axs[1].plot(range(1, len(history.history["val_accuracy"]) + 1), history.history["val_accuracy"], label='Validation accuracy', color='blue')
axs[1].set_title("Training and Validation Accuracy")
axs[1].set_ylabel("accuracy")
axs[1].set_xlabel("epoch")
axs[1].legend()
plt.tight_layout()
plt.show()


# 가장 좋은 모델 선택
best_model = load_model("../../pretrained_models/InceptionV3/h5/best_model.h5")

# 테스트 데이터에 대한 결과 출력
test_loss, test_acc = best_model.evaluate(test_generator)
print("Test Accuracy : ", test_acc)


















