import fs from 'node:fs/promises';
import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
const require=createRequire('C:/Users/admin/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/package.json');
const {Workbook,SpreadsheetFile}=await import(pathToFileURL(require.resolve('@oai/artifact-tool')).href);
const out='D:/Desktop/TTCD/ai-language-learning/outputs/robot-cost-20261021';
const w=Workbook.create();
const b=w.worksheets.add('Du toan'); const c=w.worksheets.add('So sanh gia'); const v=w.worksheets.add('Noi mua');
const rows=[
['Mạch điện tử','Raspberry Pi 5, dự trù 8GB',1,3300000,4500000,'RaspberryPi.vn; Pi Việt Nam','Chưa xác nhận giá riêng 8GB; xem so sánh'],
['Mạch điện tử','ESP32 DevKit',1,100000,200000,'Linh Kiện Việt Nam; Linh Kiện Tốt','Cần chọn bo có USB, không mua module trần'],
['Mạch điện tử','Driver cho hai động cơ',1,100000,350000,'Hshop; Linh Kiện Việt Nam','Chọn theo dòng kẹt trục; TB6612 chỉ là tham khảo'],
['Mạch điện tử','microSD 64GB',1,150000,250000,'RaspberryPi.vn; Pi Việt Nam','Ưu tiên thẻ chính hãng'],
['Mạch điện tử','Quạt và tản nhiệt Pi 5',1,200000,300000,'RaspberryPi.vn; Pi Việt Nam','Active Cooler có mốc 230.000 đồng'],
['Mạch điện tử','Bo nối và dây tín hiệu',1,150000,350000,'Linh Kiện Việt Nam; Hshop','Một bộ'],
['Hiển thị / âm thanh','Màn hình ngực cảm ứng 5–7 inch',1,1100000,1800000,'Linh Kiện Tốt; Hshop; RaspberryPi.vn','Chọn một màn hình; không cộng các phương án'],
['Hiển thị / âm thanh','LCD hiển thị khuôn mặt',1,700000,1500000,'RaspberryPi.vn; Hshop','LCD phẳng sau mica đen; cần chọn kích thước'],
['Hiển thị / âm thanh','Camera USB hoặc Pi',1,250000,800000,'RaspberryPi.vn; Pi Việt Nam','Cáp Pi 5 có thể cần mua riêng'],
['Hiển thị / âm thanh','Micro USB',1,150000,450000,'Hshop; RaspberryPi.vn','Giá dự toán micro cơ bản'],
['Hiển thị / âm thanh','Loa và khuếch đại',1,150000,350000,'Hshop; Linh Kiện Việt Nam','Một bộ'],
['Khớp / di chuyển','Động cơ giảm tốc 12V có encoder',2,250000,500000,'Hshop; DKE','Giá mỗi động cơ; chốt tỷ số truyền và tải'],
['Khớp / di chuyển','Bánh cao su 80–100 mm',2,60000,125000,'Hshop; Linh Kiện Tốt','Phải khớp trục động cơ'],
['Khớp / di chuyển','Bánh caster',1,40000,120000,'Hshop; Linh Kiện Tốt','Chọn chiều cao khớp hai bánh chính'],
['Khớp / di chuyển','Gá động cơ và khớp nối',1,100000,250000,'Hshop; xưởng cơ khí','Một bộ cho hai động cơ'],
['Khớp / di chuyển','Servo cổ, dự trù 20–35 kg·cm',1,250000,650000,'Hshop; cửa hàng robot','Chọn lại theo khối lượng đầu'],
['Khớp / di chuyển','Vòng bi hoặc mâm xoay đỡ đầu',1,100000,300000,'Cửa hàng vòng bi; xưởng cơ khí','Cần xác định kích thước trước khi mua'],
['Khớp / di chuyển','Gá cổ, trục và tay servo',1,100000,250000,'Xưởng cơ khí / in 3D','Gia công theo bản vẽ'],
['Cảm biến','ToF tránh vật phía trước',2,75000,225000,'Hshop; Linh Kiện Tốt','Giá dự toán mỗi cảm biến; chốt model'],
['Cảm biến','Cảm biến chống rơi',4,20000,50000,'Hshop; Linh Kiện Việt Nam','Cần thử với màu và vật liệu sàn'],
['Cảm biến','IMU',1,40000,150000,'Hshop; Linh Kiện Việt Nam','Chốt model trước khi đặt'],
['Nguồn pin','Pin khoảng 12V–10Ah kèm BMS',1,700000,1300000,'Đơn vị đóng pin theo yêu cầu','Cần báo giá; chưa xác minh nhà bán cụ thể'],
['Nguồn pin','Sạc phù hợp cấu hình pin',1,150000,300000,'Cùng đơn vị cung cấp pin','Chốt điện áp sạc và loại cell'],
['Nguồn pin','Hạ áp 5V cho Pi và ngoại vi',1,150000,350000,'Hshop; Linh Kiện Việt Nam','Kiểm tra tải liên tục và cấp nguồn Pi 5'],
['Nguồn pin','Nguồn riêng cho servo',1,70000,150000,'Hshop; Linh Kiện Việt Nam','Chốt theo điện áp servo'],
['Nguồn pin','Công tắc, cầu chì, dây nguồn',1,100000,250000,'Linh Kiện Việt Nam; Hshop','Một bộ'],
['Khung bên trong','Đế gầm 28–35 cm',1,250000,600000,'Xưởng CNC / laser','Dự toán gia công đơn chiếc'],
['Khung bên trong','Các tầng đỡ linh kiện',1,200000,500000,'Xưởng CNC / laser','Dự toán theo thiết kế'],
['Khung bên trong','Trụ, ke, vít và đai ốc',1,150000,350000,'Cửa hàng cơ khí; Hshop','Một bộ'],
['Khung bên trong','Gá màn hình, camera, loa',1,150000,400000,'TT Minimal; Linh Kiện Cầu Giấy','Cần file và kích thước linh kiện'],
['Vỏ ngoài','Vỏ đầu, thân, đế in thô',1,1500000,4500000,'TT Minimal; Linh Kiện Cầu Giấy','Giả định 1,5–3 kg; chốt sau khi có file'],
['Vỏ ngoài','Mica đen và viền mặt',1,150000,500000,'Xưởng mica / laser','Cần kiểm tra độ xuyên sáng'],
['Vỏ ngoài','Chà nhám, bả và sơn',1,500000,1500000,'Xưởng in 3D / sơn','Hạng mục hoàn thiện riêng'],
['Vỏ ngoài','Thiết kế file 3D',1,1000000,3000000,'TT Minimal; xưởng thiết kế','Chỉ tính nếu chưa có file phù hợp'],
['Tùy chọn','LiDAR RPLIDAR A1',1,2990000,3500000,'ProE','Không nằm trong cấu hình cơ bản'],
['Tùy chọn','Thêm khớp gật đầu',1,400000,1000000,'Hshop + xưởng cơ khí','Gồm servo thứ hai và gá; tùy chọn']];
b.getRange('A2').values=[['DỰ TOÁN LINH KIỆN ROBOT 50 CM']];
b.getRange('A3').values=[['Ngày tra cứu: 21/10/2026 • Tiền tệ: VND • Ô số lượng và đơn giá có thể chỉnh sửa']];
b.getRange('A4').values=[['Tất cả khoảng giá dưới đây là dự toán; giá niêm yết và nguồn đối chiếu nằm tại So sanh gia.']];
b.getRange('A5').values=[['Chưa gồm công lắp, lập trình, dịch vụ AI, vận chuyển và VAT phát sinh. Nơi mua dự kiến chưa đồng nghĩa còn hàng.']];
b.getRange('A7:D7').values=[['Nhóm / phạm vi','Thấp','Cao','Ghi chú']];
const groups=['Mạch điện tử','Hiển thị / âm thanh','Khớp / di chuyển','Cảm biến','Nguồn pin','Khung bên trong','Vỏ ngoài','Tùy chọn'];
groups.forEach((g,i)=>{let r=i+8;b.getRange(`A${r}`).values=[[g]];b.getRange(`B${r}:C${r}`).formulas=[[`=SUMIFS(G21:G56,A21:A56,A${r})`,`=SUMIFS(H21:H56,A21:A56,A${r})`]];});
b.getRange('A16:D18').values=[['Bên trong + khung, chưa vỏ',null,null,'Không gồm tùy chọn'],['Có vỏ, hoàn thiện và thiết kế',null,null,'Cộng toàn bộ nhóm Vỏ ngoài'],['Có thêm cả hai tùy chọn',null,null,'LiDAR + khớp gật đầu']];
b.getRange('B16:C18').formulas=[['=SUM(B8:B13)','=SUM(C8:C13)'],['=SUM(B16,B14)','=SUM(C16,C14)'],['=SUM(B17,B15)','=SUM(C17,C15)']];
b.getRange('A20:K20').values=[['Nhóm','Bộ phận / cấu hình','SL','Đơn giá thấp','Đơn giá cao','Đơn vị','Thành tiền thấp','Thành tiền cao','Nơi hỏi mua dự kiến','Ghi chú chọn mua','Loại giá / nguồn']];
b.getRange('A21:K56').values=rows.map(x=>[x[0],x[1],x[2],x[3],x[4],'cái / bộ',null,null,x[5],x[6],'Dự toán tổng hợp từ trao đổi; chưa phải báo giá']);
rows.forEach((_,i)=>{let r=i+21;b.getRange(`G${r}:H${r}`).formulas=[[`=C${r}*D${r}`,`=C${r}*E${r}`]];});
const offers=[
['Pi 5','RaspberryPi.vn','RAM chưa chốt; giá từ',3300000,'bo','Chưa gồm VAT, ship','Cần hỏi đúng 8GB','Không xếp hạng: khác RAM / giá từ','https://raspberrypi.vn/san-pham/mach-may-tinh-raspberry-pi-5?attribute_pa_ram=4gb'],
['Pi 5','Linh Kiện Tốt','RAM chưa chốt',2450000,'bo','Chưa xác định','Trang ghi hết hàng','Không dùng giá hết hàng làm phương án mua','https://www.linhkientot.vn/collections/raspberrypi'],
['Màn hình ngực','Linh Kiện Tốt','7 inch C, HDMI 1024×600 IPS cảm ứng',1320000,'cái','Chưa xác định','Cần hỏi tồn kho','Khác phiên bản H có vỏ; không so cùng mã','https://www.linhkientot.vn/collections/raspberrypi'],
['Màn hình ngực','Hshop','Waveshare 7 inch H có vỏ, 1024×600',1620000,'cái','Đã gồm thuế theo SKU HS0930V','Trang ghi còn hàng','Có vỏ; gửi từ TP.HCM','https://hshop.vn/man-hinh-7-inch-hdmi-lcd-h-cam-ung-dien-dung-waveshare-co-vo-bao-ve'],
['Màn hình ngực','RaspberryPi.vn','DSI 5 inch 720×1280, vỏ nhôm',1450000,'cái','Chưa gồm VAT, ship','Trang ghi còn hàng','Khác kích thước và cổng DSI','https://raspberrypi.vn/san-pham/man-hinh-cam-ung-dien-dung-dsi-5-inch-720-x-1280-ips-vo-hop-kim-nhom'],
['ESP32','Linh Kiện Việt Nam','DevKit V1 / nhiều tùy chọn, HH004223',140000,'bo','Chưa xác định','Cần hỏi đúng tùy chọn','Không so với module ESP-WROOM trần','https://linhkienvietnam.vn/vi-dieu-khien'],
['Driver TB6612','Hshop','Module TB6612FNG hai kênh',45000,'module','Chưa xác định','Giá trên trang danh mục','Chỉ dùng nếu phù hợp dòng động cơ','https://hshop.vn/'],
['Driver TB6612','Linh Kiện Thủ Đức','Module TB6612FNG 1,2A',35000,'module','Chưa xác định','Giá từ kết quả tìm kiếm; cần xác nhận','Cùng loại IC; chưa xác nhận cùng PCB/chất lượng','https://linhkienthuduc.com/brands/oem/page/48/'],
['Tản nhiệt Pi 5','RaspberryPi.vn','Active Cooler',230000,'cái','Chưa gồm VAT, ship','Giá phụ kiện trên trang Pi 5','Cần xác nhận phụ kiện đi kèm','https://raspberrypi.vn/san-pham/mach-may-tinh-raspberry-pi-5?attribute_pa_ram=4gb'],
['LiDAR','ProE','RPLIDAR A1M8-R6',2990000,'bộ','Chưa xác định','Có giá bán; cần hỏi tồn','Xác nhận adapter và cáp trong bộ','https://www.proe.vn/rplidar-a1m8-r6'],
['In vỏ FDM','TT Minimal','PLA/PETG/ABS, từ 2 kg',1000,'gram','Chưa xác định','Cần file 3D để báo giá','Mốc theo khối lượng; chưa gồm thiết kế/sơn','https://ttminimal.com/in-3d/'],
['In vỏ FDM','ChiDTek','PETG, thêm phí giờ in nếu >4 giờ',800,'gram','Chưa xác định','Phụ thu công bố 5.000đ/giờ (>4h)','Khác cách tính phụ thu; cần báo cùng file','https://chidtek.com/services'],
['In vỏ FDM','Linh Kiện Cầu Giấy','PLA, bảng ghi lớp 0,5 mm',1000,'gram','Chưa xác định','Bảng giá cũ; cần xác nhận','Khác độ mịn in; không xếp hạng trực tiếp','https://lkcg.vn/bao-gia-in-3d-fmd-dang-soi-gia-re-ha-noi']];
c.getRange('A2').values=[['GIÁ NIÊM YẾT VÀ PHƯƠNG ÁN THAY THẾ']];
c.getRange('A3').values=[['Tra cứu 21/10/2026. Giá từ website/kết quả tìm kiếm, chưa có xác nhận trực tiếp của cửa hàng.']];
c.getRange('A5:D5').values=[['So sánh có điều kiện','Mức A','Mức B','Chênh lệch A − B']];
c.getRange('A6').values=[['Module TB6612: Hshop / Linh Kiện Thủ Đức']];c.getRange('B6:D6').formulas=[['=D17','=D18','=B6-C6']];
c.getRange('F6').values=[['Chưa xác nhận cùng PCB; giá Thủ Đức cần kiểm tra lại.']];
c.getRange('A7').values=[['In PETG 2.000g: TT Minimal / ChiDTek']];c.getRange('B7:D7').formulas=[['=D21*2000','=D22*2000','=B7-C7']];
c.getRange('F7').values=[['Chỉ so tiền nhựa; chưa gồm phí giờ in, support, thiết kế và sơn.']];
c.getRange('A8').values=[['Màn hình và Pi: xem từng phiên bản bên dưới, không kết luận nơi rẻ nhất khi khác mã hoặc hết hàng.']];
c.getRange('A10:J10').values=[['Hạng mục','Nhà bán','Phiên bản / quy cách','Giá VND','Đơn vị giá','Thuế / phí','Tình trạng / lưu ý','Khả năng so sánh','Nguồn URL','Ngày tra cứu']];
c.getRange('A11:J23').values=offers.map(x=>[...x,new Date('2026-10-21T00:00:00Z')]);
const vendors=[
['RaspberryPi.vn','Hà Nội','Tầng 5, số 25 Hồ Mễ Trì, phường Đại Mỗ','092.707.8688','Pi, màn hình, camera, nguồn','https://raspberrypi.vn/','Địa chỉ theo website; gọi trước'],
['Pi Việt Nam / MLAB','Hà Nội','Nhà 30F9, ngõ 104 Lê Thanh Nghị, phường Bạch Mai','Xem website','Pi và phụ kiện; bộ robot nền','https://mlab.com.vn/robot-ai','Chưa lấy báo giá riêng từng linh kiện'],
['Linh Kiện Tốt','Hà Nội','Khu tập thể Không quân, Sóc Sơn','0364.199.199','Màn hình, Pi, linh kiện robot','https://www.linhkientot.vn/collections/raspberrypi','Pi 5 trên trang đang hết hàng'],
['Linh Kiện Việt Nam','Hà Nội','64 Hoàng Sâm, Cầu Giấy','0933.000.266 / 0378.000.266','ESP32, mạch điện, nguồn, cảm biến','https://linhkienvietnam.vn/vi-dieu-khien','Hỏi đúng biến thể sản phẩm'],
['Hshop','TP.HCM / mua online','269/20 Lý Thường Kiệt, phường Phú Thọ','028.6670.4455','Driver, động cơ, bánh, servo, màn hình','https://hshop.vn/','Có thể đặt giao Hà Nội; xác nhận phí ship'],
['ProE','Online','Xác nhận địa chỉ khi đặt','0938.946.849','RPLIDAR A1','https://www.proe.vn/rplidar-a1m8-r6','Chưa xác nhận điểm nhận hàng Hà Nội'],
['DKE','Online','Xác nhận khi liên hệ','Xem website','Động cơ có encoder','https://dke.vn/dong-co/dong-co-dc/dong-co-360-12vdc-encoder','Chỉ là nơi hỏi; chưa chốt model và giá'],
['TT Minimal','Hà Nội theo website','Xác nhận khi liên hệ','Xem website','In FDM, thiết kế file 3D','https://ttminimal.com/in-3d/','Gửi cùng file để nhận giá trọn gói'],
['Linh Kiện Cầu Giấy','Hà Nội','20 ngách 1 ngõ 46 Trần Quốc Hoàn, Cầu Giấy','Xem website','In FDM, PLA','https://lkcg.vn/bao-gia-in-3d-fmd-dang-soi-gia-re-ha-noi','Bảng địa chỉ/giá cũ; gọi xác nhận'],
['ChiDTek','Online','Chưa xác nhận địa điểm nhận Hà Nội','0876.165.070','In PETG/PLA và thiết kế','https://chidtek.com/services','Có phí giờ in theo website'],
['Linh Kiện Thủ Đức','Online','Chưa xác nhận điểm bán Hà Nội','Xem website','Module TB6612','https://linhkienthuduc.com/brands/oem/page/48/','Trang mở trực tiếp lỗi; giá lấy từ kết quả tìm kiếm']];
v.getRange('A2').values=[['NƠI MUA VÀ LIÊN HỆ']];v.getRange('A3').values=[['Danh sách để hỏi mua; không phải xác nhận mọi linh kiện đều có sẵn tại từng cửa hàng.']];
v.getRange('A5:H5').values=[['Nhà bán / xưởng','Khu vực','Địa chỉ công bố','Điện thoại','Hạng mục có thể hỏi','Nguồn URL','Lưu ý','Ngày tra cứu']];
v.getRange('A6:H16').values=vendors.map(x=>[...x,new Date('2026-10-21T00:00:00Z')]);
for(const [s,end,head] of [[b,56,20],[c,23,10],[v,16,5]]){
 s.showGridLines=false;s.getRange(`A1:${s===b?'K':s===c?'J':'H'}${end}`).format.font={name:'Arial',size:11,color:'#243247'};
 s.getRange('A2').format.font={bold:true,size:16,color:'#17365D'};
 s.getRange(`A${head}:${s===b?'K':s===c?'J':'H'}${head}`).format.fill='#17365D';s.getRange(`A${head}:${s===b?'K':s===c?'J':'H'}${head}`).format.font={bold:true,color:'#FFFFFF'};
 s.getRange(`A${head}:${s===b?'K':s===c?'J':'H'}${end}`).format.rowHeight=48;
 s.getRange(`A${head}:${s===b?'K':s===c?'J':'H'}${end}`).format.wrapText=true;
 s.getRange(`A1:${s===b?'K':s===c?'J':'H'}${end}`).format.verticalAlignment='center';
 s.freezePanes.freezeRows(head);s.tables.add(`A${head}:${s===b?'K':s===c?'J':'H'}${end}`,true,s===b?'BudgetItems':s===c?'PriceOffers':'Vendors');
}
const widths=(s,arr)=>arr.forEach((n,i)=>s.getRange(`${String.fromCharCode(65+i)}1:${String.fromCharCode(65+i)}60`).format.columnWidth=n);
widths(b,[38,43,20,35,17,14,19,19,41,55,45]);widths(c,[29,25,48,19,14,27,42,52,65,17]);widths(v,[27,25,58,30,48,65,56,17]);
b.getRange('D21:E56').setNumberFormat('#,##0');b.getRange('G21:H56').setNumberFormat('#,##0');b.getRange('B8:C18').setNumberFormat('#,##0');b.getRange('C21:E56').format.fill='#FFF2CC';
b.getRange('A7:D7').format.fill='#DCE6F1';b.getRange('A7:D7').format.font={bold:true};b.getRange('A16:D18').format.fill='#E2EFDA';b.getRange('A16:C18').format.font={bold:true};b.getRange('A7:D18').format.rowHeight=27;
c.getRange('D11:D23').setNumberFormat('#,##0');c.getRange('B6:D7').setNumberFormat('#,##0');c.getRange('A5:D5').format.fill='#DCE6F1';c.getRange('J11:J23').setNumberFormat('dd/mm/yyyy');v.getRange('H6:H16').setNumberFormat('dd/mm/yyyy');
w.recalculate();
console.log((await w.inspect({kind:'table',range:'Du toan!A7:D18',include:'values,formulas',tableMaxRows:12,tableMaxCols:4})).ndjson);
console.log((await w.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#NUM!',options:{useRegex:true,maxResults:20}})).ndjson);
for(const [s,r,name] of [['Du toan','A7:H18','budget'],['So sanh gia','A10:H15','prices'],['Noi mua','A5:E10','vendors']]){const p=await w.render({sheetName:s,range:r,scale:1.5,format:'png'});await fs.writeFile(`${out}/${name}.png`,new Uint8Array(await p.arrayBuffer()));}
await (await SpreadsheetFile.exportXlsx(w)).save(`${out}/Bang_gia_linh_kien_robot.xlsx`);
console.log('EXPORTED');
