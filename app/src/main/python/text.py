import cv2
import numpy as np

# 加载预训练的人体检测模型
hog = cv2.HOGDescriptor()
hog.setSVMDetector(cv2.HOGDescriptor_getDefaultPeopleDetector())

def detect_people(image):
    # 将图像转换为灰度
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # 检测人体
    boxes, weights = hog.detectMultiScale(gray, winStride=(8, 8))

    # 画出检测到的框
    for (x, y, w, h) in boxes:
        cv2.rectangle(image, (x, y), (x + w, y + h), (0, 255, 0), 2)

    # 返回处理后的图像和检测结果
    return image, len(boxes) > 0