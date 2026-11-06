# Thuật toán lập lộ trình

Tài liệu này mô tả bài toán, mô hình, thuật toán TripFair dùng để lập lộ trình (cá nhân và nhóm), cách đánh giá và kết quả đánh giá. Code nằm ở [backend/src/planner/](../backend/src/planner/) (thuần TypeScript, không phụ thuộc NestJS hay database) và [backend/src/trips/planning.service.ts](../backend/src/trips/planning.service.ts) (nối thuật toán với dữ liệu thật).

## 1. Bài toán

Đầu vào:

- **Thành viên** `m`: số tiền còn dành cho các điểm `A_m` (ngân sách trừ phần tiền phòng), danh sách sở thích. Chuyến một mình là nhóm có 1 người.
- **Địa điểm ứng viên** `p`: vị trí, thời gian ghé `v_p`, giá mỗi người `c_p` (mức cao của khoảng giá, để không bao giờ vượt ngân sách), giờ mở cửa, các ngày đóng cửa, sở thích phù hợp, điểm đánh giá, ai đã bình chọn, và khu có vé bao trọn (vd. Cầu Vàng nằm trong Bà Nà Hills).
- **Chuyến đi**: số ngày `D`, khung giờ mỗi ngày (mặc định 08:00–21:00), thời gian nghỉ tối thiểu giữa hai điểm `g` (mặc định 30 phút), nơi xuất phát và kết thúc mỗi ngày (khách sạn, nếu có).

Đầu ra: các điểm của từng ngày theo thứ tự, giờ bắt đầu, ai đi điểm nào và trả bao nhiêu.

Ràng buộc cứng:

1. Không ai tiêu quá `A_m` (ngân sách 0đ thì chỉ đi điểm miễn phí).
2. Mỗi ngày kết thúc (tính cả đường về khách sạn) trước giờ kết thúc ngày: tổng thời gian ≤ T.
3. Mỗi lần ghé nằm trong giờ mở cửa; không xếp điểm vào ngày nó đóng cửa.
4. Mỗi điểm ghé tối đa một lần.

Mục tiêu: tổng giá trị các điểm mỗi người đi càng cao càng tốt, và giá trị chia đều giữa các thành viên (công bằng).

## 2. Mô hình

**Giá trị một điểm với một người** (`placeValue`, [scoring.ts](../backend/src/planner/scoring.ts)), trong khoảng 0,2–1,5:

```
sởThích = 0,5                              nếu người đó không chọn sở thích nào
        = 0,6 + 0,4 · min(1, khớp / 2)     nếu khớp ít nhất 1 sở thích
        = 0,2                              nếu không khớp
giáTrị  = 0,7 · sởThích + 0,3 · (đánhGiá / 5) + (0,5 nếu người đó bình chọn điểm này)
```

**Chi phí**: mỗi người trả `c_p` ở điểm họ đi, trừ khi họ cũng đi khu có vé bao trọn điểm đó (khi đó 0đ). Ai không đủ tiền thì không đi điểm đó, những người khác vẫn đi.

**Thời gian di chuyển** khi lập lộ trình: đường chim bay × 1,4 (hệ số đường vòng) với tốc độ 25 km/h ([travel.ts](../backend/src/planner/travel.ts)). Thuật toán gọi hàm này hàng nghìn lần nên không gọi API. Lộ trình đã lập được tính lại bằng Google Routes (mục 4).

**Mức hài lòng** của một người (`satisfaction`), 0–1: tổng giá trị các điểm người đó đi chia cho tổng giá trị `K` điểm ứng viên họ thích nhất, với `K` là số điểm của cả lộ trình. 100% nghĩa là mọi điểm người đó đi đều là điểm họ thích nhất. App hiển thị số này theo %.

**Độ công bằng**: chỉ số Jain trên mức hài lòng của các thành viên, `J = (Σx)² / (n · Σx²)`, bằng 1 khi mọi người hài lòng như nhau, bằng 1/n khi chỉ một người hài lòng.

## 3. Các bước của thuật toán ([plan.ts](../backend/src/planner/plan.ts))

### 3.1 Greedy

Lặp lại cho tới khi không thêm được điểm nào:

1. Với mỗi điểm chưa chọn: những người đi là mọi người còn đủ tiền; tìm ngày và vị trí chèn làm lộ trình dài thêm ít nhất mà vẫn hợp lệ (cheapest insertion, [schedule.ts](../backend/src/planner/schedule.ts)).
2. Chọn điểm tốt nhất: điểm miễn phí (với những người đi) đứng trước mọi điểm có phí; giữa các điểm có phí, tỉ lệ tổng giá trị / tổng chi phí cao hơn đứng trước. Điểm miễn phí có tỉ lệ vô cực, nên không bao giờ chia cho 0.
3. Chèn điểm đó vào vị trí đã tìm.

### 3.2 Local search

1. **2-opt** từng ngày: đảo ngược một đoạn các điểm nếu ngày kết thúc sớm hơn (bớt đường đi).
2. **Greedy lần hai** cho phần thời gian vừa được giải phóng.
3. **Đổi điểm (1-swap)**: bỏ một điểm đã chọn, thay bằng một điểm chưa chọn nếu tổng giá trị của nhóm tăng và mọi ngân sách vẫn đủ. Bước này sửa lỗi điển hình của Greedy: một điểm miễn phí hoặc rẻ được chọn sớm chiếm hết thời gian hay tiền của một điểm đáng giá hơn.
4. 2-opt và Greedy thêm một lần nữa.

### 3.3 Fairness check

Ngân sách là ràng buộc cứng ở mọi bước, nên "không ai vượt ngân sách cá nhân" luôn đúng; bước này làm phần chi tiêu và mức hài lòng đều hơn:

1. Mỗi người tham gia mọi điểm đã chọn mà họ đủ tiền (vd. vé khu vừa mua làm một điểm bên trong thành 0đ).
2. **Đổi phần tham gia**: bắt đầu từ người kém hài lòng nhất, một người rời một điểm họ ít thích (điểm đó vẫn còn người khác đi) để đủ tiền đi một điểm họ thích hơn. Chi phí tính theo từng người nên không ai khác bị ảnh hưởng.
3. Người kém hài lòng nhất được thêm điểm họ thích nhất mà họ đủ tiền, miễn là mức hài lòng thấp nhất của nhóm tăng lên.

Cuối cùng kết quả được kiểm tra lại: ai vượt ngân sách hoặc ngày nào không hợp lệ thì báo lỗi lập trình (không bao giờ trả về lộ trình sai).

## 4. Từ thuật toán tới lộ trình thật ([planning.service.ts](../backend/src/trips/planning.service.ts))

1. **Ứng viên**: các điểm đã chọn hoặc đưa vào bình chọn; chưa có thì tìm "điểm tham quan nổi tiếng" và "quán ăn ngon nổi tiếng" của thành phố trên Google Places (2 lượt Text Search, lấy luôn giá và giờ mở cửa).
2. **Giá**: vé đối tác → `priceRange` của Google → loại điểm thường miễn phí (bãi biển, công viên, chùa, cầu…) = 0đ → bảng quy đổi `priceLevel` → mức mặc định theo nhóm.
3. **Giờ mở cửa**: Google trả giờ cho từng thứ trong tuần; ngày nào điểm đóng cửa thì đưa vào `closedDays`.
4. **Tiền phòng**: 2 người một phòng theo thứ tự vào nhóm, tiền mỗi phòng chia đều cho người ở; phần này trừ khỏi ngân sách trước khi lập lộ trình.
5. Chạy thuật toán (mục 3).
6. **Tính lại giờ bằng Google Routes**: mỗi ngày một request `computeRoutes` (khách sạn → các điểm → khách sạn), lấy thời gian lái xe thật cho từng chặng. Nếu ngày không còn kịp (về muộn hoặc tới khi điểm đã đóng cửa), bỏ điểm cuối ngày cho tới khi kịp; bỏ một khu có vé thì bỏ luôn các điểm bên trong nó (giá của chúng dựa vào vé khu). Không có key hoặc Google lỗi thì giữ ước tính.
7. Lưu lộ trình, phần chi của từng người và mức hài lòng.

## 5. Lời giải tối ưu để so sánh ([exact.ts](../backend/src/planner/exact.ts))

Đề bài yêu cầu so với ILP (OR-Tools). OR-Tools cần Python hoặc thư viện native, nên TripFair dùng **tìm kiếm vét cạn có cắt nhánh** cho ra cùng lời giải tối ưu với ILP trên bộ dữ liệu nhỏ (tối đa 12 điểm), viết bằng TypeScript để chạy chung bộ test.

Mô hình ILP tương đương (để trình bày trong báo cáo):

- Biến: `y_p ∈ {0,1}` (điểm `p` có trong lộ trình), `x_{m,p} ∈ {0,1}` (người `m` đi điểm `p`), `z_{p,d} ∈ {0,1}` (điểm `p` thuộc ngày `d`), cùng các biến thứ tự và thời điểm của bài toán định tuyến có khung thời gian (VRPTW).
- Mục tiêu: `max Σ_m Σ_p giáTrị(m,p) · x_{m,p}`.
- Ràng buộc: `x_{m,p} ≤ y_p`; `Σ_d z_{p,d} = y_p`; `Σ_p chiPhí(m,p) · x_{m,p} ≤ A_m` (chi phí bằng 0 khi người đó đi khu bao trọn); thời gian và giờ mở cửa của từng ngày như ràng buộc 2–3 ở mục 1.

Cách vét cạn:

1. Với mọi tập điểm (bit mask), tìm thứ tự trong một ngày kết thúc sớm nhất bằng cách thử mọi hoán vị. Tập nào không xếp được trong một ngày thì mọi tập chứa nó cũng không xếp được (bỏ một điểm không bao giờ làm ngày kết thúc muộn hơn), nên các tập đó bị bỏ qua.
2. Ghép các tập một ngày thành mọi tập điểm chia được cho `D` ngày (có tính ngày đóng cửa).
3. Với mỗi tập, mỗi người chọn tập con họ đủ tiền có tổng giá trị lớn nhất (bài toán cái túi, giải bằng duyệt mọi tập con). Chi phí theo từng người nên lựa chọn của mỗi người độc lập với nhau.
4. Chọn tập có tổng giá trị lớn nhất; hòa thì chọn lộ trình công bằng hơn (Jain cao hơn), rồi ít điểm hơn.

Độ phức tạp là hàm mũ theo số điểm, nên chỉ dùng để đánh giá, không dùng trong app.

## 6. Đánh giá

Chạy trong `backend/`:

```bash
npm run bench:planner
```

```bash
npm run bench:planner -- --seed 7
```

Chương trình ([scripts/benchmark-planner.ts](../backend/scripts/benchmark-planner.ts)) sinh 20 bộ dữ liệu ngẫu nhiên cho mỗi kịch bản từ một seed (cùng seed luôn ra cùng số liệu): điểm rải trong bán kính ~7 km quanh Đà Nẵng, nhiều điểm miễn phí và vài vé lớn, 30% điểm có giờ mở cửa, 15% điểm nằm trong khu có vé, ngân sách mỗi người ngẫu nhiên từ 0đ tới 1,2 triệu/ngày, mỗi người bình chọn ngẫu nhiên 20% số điểm.

Các chỉ số:

- **Tổng giá trị**: tổng giá trị các điểm mỗi người đi (mục tiêu của ILP); **% tối ưu** so với lời giải vét cạn trên cùng bộ dữ liệu.
- **Hài lòng TB**: mức hài lòng trung bình của các thành viên.
- **Jain**: độ công bằng của mức hài lòng.
- **Lệch ngân sách**: trung bình `|A_m − chi tiêu| / A_m`, tức phần ngân sách chưa dùng tới (người có 0đ tính là 0).
- **Vượt ngân sách**: số thành viên tiêu quá ngân sách (luôn phải bằng 0).
- **Thời gian**: thời gian chạy trung bình một lần (ms), trên máy phát triển (Windows, Node.js 24).

Kết quả với seed 2026 (ngày 2026-11-07):

**6 điểm, 1 người, 1 ngày**

| Thuật toán | Tổng giá trị | % tối ưu | Hài lòng TB | Jain | Lệch ngân sách | Vượt ngân sách | Số điểm | Thời gian (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Greedy thuần | 2.25 | 93.9% | 86.9% | 1.000 | 38.8% | 0 | 3.8 | 0.180 |
| Greedy + Local search + Fairness | 2.35 | 99.0% | 90.6% | 1.000 | 27.5% | 0 | 3.9 | 0.310 |
| Tối ưu (vét cạn) | 2.38 | 100.0% | 91.3% | 1.000 | 21.4% | 0 | 3.9 | 8.130 |

**8 điểm, 3 người, 1 ngày**

| Thuật toán | Tổng giá trị | % tối ưu | Hài lòng TB | Jain | Lệch ngân sách | Vượt ngân sách | Số điểm | Thời gian (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Greedy thuần | 8.84 | 95.3% | 70.3% | 0.925 | 34.5% | 0 | 4.8 | 0.382 |
| Greedy + Local search + Fairness | 9.04 | 97.3% | 70.9% | 0.920 | 26.4% | 0 | 5.0 | 0.830 |
| Tối ưu (vét cạn) | 9.25 | 100.0% | 70.7% | 0.907 | 21.3% | 0 | 5.2 | 226.448 |

**10 điểm, 4 người, 2 ngày**

| Thuật toán | Tổng giá trị | % tối ưu | Hài lòng TB | Jain | Lệch ngân sách | Vượt ngân sách | Số điểm | Thời gian (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Greedy thuần | 18.73 | 98.1% | 74.6% | 0.901 | 20.5% | 0 | 9.2 | 1.402 |
| Greedy + Local search + Fairness | 18.86 | 98.8% | 75.2% | 0.903 | 16.8% | 0 | 9.2 | 1.816 |
| Tối ưu (vét cạn) | 19.06 | 100.0% | 75.3% | 0.906 | 15.7% | 0 | 9.4 | 3890.958 |

Nhận xét:

- Bản đầy đủ đạt 97–99% tối ưu, cao hơn Greedy thuần 0,7–5 điểm phần trăm, trong khi chỉ chạy dưới 2 ms; lời giải vét cạn mất tới gần 4 giây với 10 điểm và tăng theo hàm mũ, không dùng được trong app.
- Ngân sách được dùng tốt hơn: lệch ngân sách giảm khoảng 4–11 điểm phần trăm so với Greedy thuần. Benchmark chưa tách riêng đóng góp của từng bước (đổi điểm, đổi phần tham gia).
- Không thuật toán nào để ai vượt ngân sách (cột "Vượt ngân sách" luôn 0), kể cả người có 0đ.
- Jain của bản đầy đủ có lúc thấp hơn Greedy thuần một chút (0,920 so với 0,925): tổng giá trị tăng nhưng phần tăng không chia đều. Lời giải tối ưu theo tổng giá trị cũng có Jain thấp hơn, cho thấy hai mục tiêu tổng và công bằng có đánh đổi.
- Phiên bản đầu của thuật toán (chỉ 2-opt và thêm điểm cho người kém hài lòng) cho kết quả giống hệt Greedy thuần trên cả ba kịch bản, vì Greedy đã thêm mọi điểm còn vừa thời gian và có người trả được. Benchmark này là lý do thêm hai phép đổi ở mục 3.2 và 3.3.

Giới hạn: dữ liệu ngẫu nhiên chứ không phải dữ liệu người dùng thật; thời gian di chuyển trong đánh giá là ước tính đường chim bay (không gọi Google Routes); số điểm nhỏ để lời giải vét cạn chạy được.

## 7. Kiểm thử

- `npm test` chạy unit test của thuật toán ([plan.spec.ts](../backend/src/planner/plan.spec.ts), [exact.spec.ts](../backend/src/planner/exact.spec.ts), [scoring.spec.ts](../backend/src/planner/scoring.spec.ts)): ngân sách 0đ, điểm miễn phí trước, giờ mở cửa, ngày đóng cửa, vé khu, khung giờ, bình chọn, đổi điểm, đổi phần tham gia, lời giải tối ưu không kém thuật toán.
- `npm run test:e2e` chạy [trips.e2e-spec.ts](../backend/test/trips.e2e-spec.ts) trên cả API: tổng dồn và số tiền còn lại khớp nhau, không ai vượt ngân sách, khách sạn và Google Routes (giả lập).
