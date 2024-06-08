import os
from keras.preprocessing.image import ImageDataGenerator
from mobilevit.models.mobilevit import get_mobilevit_model
import matplotlib.pyplot as plt
from keras.callbacks import EarlyStopping, ModelCheckpoint, LearningRateScheduler
from keras.optimizers import SGD
from keras.models import load_model
import tensorflow as tf


base_dir = '../../final_food'

train_dir = os.path.join(base_dir, 'train')
val_dir = os.path.join(base_dir, 'validation')
test_dir = os.path.join(base_dir, 'test')


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


train_generator = train_datagen.flow_from_directory(
    train_dir,
    target_size = (256, 256),
    batch_size = 32,
)

val_generator = val_datagen.flow_from_directory(
    val_dir,
    target_size = (256, 256),
    batch_size = 32,
    shuffle=True,
)

test_generator = test_datagen.flow_from_directory(
    test_dir,
    target_size = (256, 256),
    batch_size = 32,
)

model = get_mobilevit_model(
    model_name='mobilevit_s',
    image_shape=(256,256,3),
    num_classes=151
)

# Early stopping 설정
early_stopping = EarlyStopping(monitor='val_accuracy', patience=4, verbose=1, restore_best_weights=True)

# ModelCheckpoint 설정
checkpoint_filepath = '../../pretrained_models/MobileVit/h5/best_model.h5'
checkpoint = ModelCheckpoint(checkpoint_filepath, monitor='val_accuracy', save_best_only=True, verbose=1)

opt = SGD(lr=.01, momentum=.9)

# 학습률 스케쥴러 설정
def schedule(epoch):
    if epoch < 15:
        return .01
    elif epoch < 28:
        return .002
    else:
        return .0004
lr_scheduler = LearningRateScheduler(schedule)

# 모델 컴파일
model.compile(optimizer=opt,
              loss='categorical_crossentropy',
              metrics=['accuracy'])

# 모델 학습
history = model.fit(train_generator, epochs=30, batch_size=2, validation_data=val_generator, callbacks=[early_stopping, checkpoint, lr_scheduler])


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
best_model = load_model("../../pretrained_models/Mobilevit/h5/best_model.h5")

# 테스트 데이터에 대한 결과 출력
test_loss, test_acc = best_model.evaluate(test_generator)
print("Test Accuracy : ", test_acc)



