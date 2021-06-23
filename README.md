# ARRuler

ARRuler là một ứng dụng có thể đo khoảng cách của hai điểm ngẫu nhiên cũng như mô phỏng chế độ xem AR để người dùng đo phòng, vật thể, cửa ra vào, cửa sổ ...

Dự án này chỉ là bản demo cho Luận văn tốt nghiệp , vì vậy tính hiệu quả và quản lý của mã nguồn không được bao gồm trong dự án này. Dự án sử dụng ARCore, do Google tạo ra, để phát triển ứng dụng ARRuler.

## Cài đặt

- Đầu tiên, để sử dụng mã nguồn này, nhà phát triển phải tải xuống android studio tại: https://developer.android.com/studio

- Và sau đó, sao chép repo này vào máy tính của bạn và mở nó trong studio android

- Cuối cùng, kết nối cáp usb với điện thoại di động của bạn và nhấp vào Chạy ứng dụng

## Demo

Có một số tính năng trong ứng dụng:
- Đo khoảng cách của hai điểm, được đặt bằng việc chọn nút bấm của người dùng
- Đo vật thể (chiều rộng, chiều dài, chiều cao). Ứng dụng cũng hiển thị hình khối bao quanh vật thể. Sau đó, một bảng thông tin sẽ xuất hiện phía trên đối tượng
- Đo phòng (độ dài từng đoạn thẳng, diện tích, thể tích). Ứng dụng sẽ hiển thị bảng thông tin về phòng khi kết thúc quá trình đo
- Đo cửa (chiều dài, chiều cao, diện tích).
- Tạo chế độ xem 3D của toàn bộ căn phòng, bao gồm cả cửa ra vào.
- Đo cửa sổ (chưa cập nhật)

## Cách sử dụng

- Đầu tiên, người dùng hướng camera xuống bề mặt sàn, đảm bảo sàn phẳng và ánh sáng đầy đủ.
- Sau đó, người dùng chạm vào bề mặt, rồi chạm vào nút bấm để đo chiều cao của căn phòng
- Có một nút cài đặt, người dùng có thể chọn để đo phòng hoặc đo vật thể.
- Khi người dùng kết thúc việc đo phòng, nhấp vào nút Generate để xem hình ảnh 3D của phòng.

## Phiên bản Gradle
```text
androidx.appcompat:appcompat:1.2.0
androidx.constraintlayout:constraintlayout:2.0.4
androidx.core:core-ktx:1.3.2
com.google.ar.sceneform:core:1.17.1
com.google.ar:core:1.23.0
androidx.test.espresso:espresso-core:3.3.0
androidx.test.ext:junit:1.1.2
junit:junit:4.13.2
junit:junit:4.13.2
com.google.android.material:material:1.3.0
androidx.recyclerview:recyclerview:1.1.0
com.google.ar.sceneform.ux:sceneform-ux:1.17.1
```
## Bản quyền
[HCMUS](https://hcmus.edu.vn/)