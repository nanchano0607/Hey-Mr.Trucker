import { Link } from "react-router-dom";

export default function RefundPolicyPage() {
  return (
    <div className="font-sans min-h-screen bg-gray-500 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto bg-gray-200 rounded-lg shadow-md p-8 mt-16">
        <h1 className="text-3xl font-bold mb-8">청약철회 및 환불 정책</h1>

        <div className="prose prose-sm max-w-none space-y-6">
          <section>
            <h2 className="text-2xl font-bold mb-4">1. 청약철회 기간</h2>
            <div className="text-gray-700 space-y-3">
              <p>
                <strong>전자상거래 등에서의 소비자보호에 관한 법률</strong>에 따라 상품 수령일로부터{" "}
                <strong className="text-red-600">7일 이내</strong>에 청약철회를 신청하실 수 있습니다.
              </p>
              <p>
                단, 상품의 내용이 표시·광고 내용과 다르거나 계약 내용과 다르게 이행된 경우에는 상품을 공급받은 날로부터{" "}
                <strong>3개월 이내</strong>, 그 사실을 안 날 또는 알 수 있었던 날로부터 <strong>30일 이내</strong>에
                청약철회를 할 수 있습니다.
              </p>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">2. 청약철회 제한 사유</h2>
            <p className="text-gray-700 mb-3">다음 각 호의 경우에는 청약철회가 제한됩니다.</p>
            <ul className="list-disc list-inside text-gray-700 space-y-2">
              <li>소비자의 책임 있는 사유로 상품이 멸실 또는 훼손된 경우</li>
              <li>소비자의 사용 또는 일부 소비에 의하여 상품의 가치가 현저히 감소한 경우</li>
              <li>시간의 경과에 의하여 재판매가 곤란할 정도로 상품 가치가 현저히 감소한 경우</li>
              <li>
                상품의 포장을 개봉한 경우 (단, 상품 확인을 위한 개봉은 청약철회 가능하나, 상품 택(tag) 제거 시 교환/반품이
                불가능합니다)
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">3. 환불 절차</h2>
            <div className="text-gray-700 space-y-3">
              <div className="bg-blue-50 border border-blue-200 rounded p-4">
                <p className="font-semibold mb-2">📌 환불 신청 방법</p>
                <ol className="list-decimal list-inside space-y-1">
                  <li>마이페이지 → 주문내역 → 상세보기 "반품 요청" 버튼 클릭</li>
                  <li>반품 사유 선택 (단순변심 or 제품하자)</li>
                  <li>회사의 승인 후 반품 송장번호 안내</li>
                  <li>상품 회수 완료 후 환불 처리</li>
                </ol>
              </div>

              <p className="mt-4">
                <strong>환불 소요 기간:</strong> 상품 회수 확인 후 영업일 기준 <strong>3~5일 이내</strong> 처리됩니다.
              </p>
              <p>
                <strong>결제수단별 환불:</strong>
              </p>
              <ul className="list-disc list-inside space-y-1 ml-4">
                <li>신용카드: 카드사 승인 취소 (영업일 3~5일 소요)</li>
                <li>실시간 계좌이체: 고객 계좌로 환불 (영업일 3일 이내)</li>
                <li>가상계좌: 환불 계좌 확인 후 입금 (영업일 3일 이내)</li>
              </ul>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">4. 반품 배송비</h2>
            <div className="text-gray-700 space-y-3">
              <div className="bg-yellow-50 border border-yellow-200 rounded p-4">
                <p className="font-semibold mb-2">📦 배송비 부담 기준</p>
                <ul className="list-disc list-inside space-y-2">
                  <li>
                    <strong>단순 변심:</strong> 왕복 배송비 <strong className="text-red-600">7,000원</strong> 고객 부담
                    (편도 3,500원 × 2)
                  </li>
                  <li>
                    <strong>상품 하자, 오배송:</strong> 배송비 <strong className="text-blue-600">회사 부담</strong> (전액
                    환불)
                  </li>
                </ul>
              </div>

              <p className="mt-4">
                단순 변심으로 인한 반품 시, 환불 금액에서 배송비가 차감되어 처리됩니다.
              </p>
            </div>
          </section>


          <section>
            <h2 className="text-2xl font-bold mb-4">5. 포인트 및 쿠폰 환불</h2>
            <div className="text-gray-700 space-y-3">
              <ul className="list-disc list-inside space-y-2">
                <li>
                  <strong>포인트:</strong> 결제 시 사용한 포인트는 환불 처리 시 자동으로 재적립됩니다.
                </li>
                <li>
                  <strong>쿠폰:</strong> 사용한 쿠폰은 유효기간 내인 경우 재발급되며, 유효기간이 지난 경우 재발급되지
                  않습니다.
                </li>
              </ul>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">6. 고객센터 문의</h2>
            <div className="text-gray-700 space-y-2 bg-gray-100 p-4 rounded">
              <p>
                <strong>문의 방법:</strong> 우측하단 카카오톡 채널 또는 이메일 문의
              </p>
              <p>
                <strong>운영 시간:</strong> 평일 10:00 ~ 18:00 (주말 및 공휴일 휴무)
              </p>
              <p>
                <strong>이메일:</strong> example@gmail.com
              </p>
            </div>
          </section>

          <section className="mt-8 pt-6 border-t border-gray-400">
            <p className="text-sm text-gray-600">
              본 환불정책은 <strong>전자상거래 등에서의 소비자보호에 관한 법률</strong> 및 관련 법령에 따라 운영되며,
              법령이 정한 바에 따릅니다.
            </p>
            <p className="text-sm text-gray-600 mt-2">최종 수정일: 2026년 1월 12일</p>
          </section>

          <div className="mt-8 text-center">
            <Link
              to="/terms-of-service"
              className="inline-block px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
            >
              ← 이용약관으로 돌아가기
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
