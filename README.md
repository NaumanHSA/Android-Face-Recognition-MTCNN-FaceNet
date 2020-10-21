# Android Face Recognition
Face recognition is one of the other biometric solutions which can be used for identification and authentication perposes using camera, whether it's a smartphones camera or some IP surveillance camera. Facial recognition softwares detect faces in images, extract different features based on the facial landmarks and later compare them against other stored faces in a database. It then computes a similarity score which is then compared to a certain threshold. It can also be applied to recorded videos and images as well can be run on live camera.


# Models
This project includes two models.
  
1. MTCNN (pnet.tflite, rnet.tflite, onet.tflite)
This model is used to detect faces in an image. It inputs a Bitmap and outputs bounding box coordinates. We will use this model for detecting faces in an image.  

2. MobileFaceNet(MobileFaceNet.tflite)
This model is used to compute the similarity score for two faces. It inputs two Bitmaps and outputs a float score. We will use this model to judge whether two face images are one person.


# BUILD
After putting .tflite in your assets directory, remember to add this code to your gradle:  
aaptOptions {  
　　noCompress "tflite"  
}  

The live camera feature uses android OpenCV, so you'd probably need to configure openCV in your project. 
1. You can either use the following link for direct configuration withoout NDK dependency which is quite easy.
https://github.com/quickbirdstudios/opencv-android

2. OR you can download the android version of openCV and import it as a module in your android project. Don't forget to create jniLibs folder inside \app\src\main\ and paste all folders (archetictures) from OpenCV-android-sdk\sdk\native\libs directory to jniLibs folder.

In the app/build.gradle file, specify the following things:

android {
    ...
    ...
    
    sourceSets {
            main {
                jni {
                    srcDirs 'src\\main\\jni', 'src\\main\\jniLibs'
                }
            }
        }

        splits {
            abi {
                enable true
                reset()
                include 'x86', 'armeabi-v7a', 'x86_64', 'arm64-v8a'
                universalApk true
            }
        }
        
    ...
    ...
}

# App Features
This app has two modules:
1. We can register a face which will save a face in the local app directory for future use. We can register a face by taking a new photo from camera or by choosing from phone's gallery providing a name.

2. We can recognize faces in an image by taking a new photo using camera or choosing from gallery. We can recognize faces in live camera streaming as well. The app compares faces in new images with those of saved in database (regsitered faces).
  
  
# SCREEN SHOT
<img src="https://github.com/NaumanHSA/Android-Face-Recognition-MTCNN-FaceNet/blob/master/ScreenShot/SS1.jpg" width=375/>
<img src="https://github.com/NaumanHSA/Android-Face-Recognition-MTCNN-FaceNet/blob/master/ScreenShot/SS2.jpg" width=375/>


# References
https://github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing
This project is the extension of the above project. Shout out for this guy who has made this amazing android project, collecting different models together. 

https://github.com/ipazc/mtcnn 

https://github.com/davidsandberg/facenet 
  
https://github.com/jiangxiluning/facenet_mtcnn_to_mobile  
Here's how to convert .tflite.
  
https://github.com/sirius-ai/MobileFaceNet_TF  
We will use this model for comparing faces. It has been designed for mobile devices. The original Facenet is about 90MBs which is too heavy for mobile phones.
