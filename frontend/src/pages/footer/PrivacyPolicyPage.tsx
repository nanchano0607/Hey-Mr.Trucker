export default function PrivacyPolicyPage() {
  return (
    <div className="font-sans min-h-screen bg-gray-500 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto bg-gray-200 rounded-lg shadow-md p-8 mt-16">
        <h1 className="text-3xl font-bold mb-8">개인정보처리방침</h1>

        <div className="prose prose-sm max-w-none space-y-6">
          <section>
            <p className="text-gray-700">
              HEY! MR. TRUCKER(이하 "회사")는 이용자의 개인정보를 중요시하며, 관련 법령을 준수합니다.
            </p>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">1. 수집하는 개인정보 항목</h2>
            <p className="text-gray-700 mb-3">회사는 서비스 제공을 위해 다음의 개인정보를 수집할 수 있습니다.</p>
            
            <h3 className="text-xl font-semibold mb-2">필수 항목</h3>
            <ul className="list-disc list-inside text-gray-700 space-y-1 mb-4">
              <li>이름</li>
              <li>휴대전화번호</li>
              <li>배송지 주소</li>
              <li>이메일 주소</li>
            </ul>

            <h3 className="text-xl font-semibold mb-2">결제 관련 정보</h3>
            <ul className="list-disc list-inside text-gray-700 space-y-1">
              <li>결제 승인 정보</li>
              <li className="text-sm">
                ※ 신용카드 번호 등 민감한 결제 정보는 회사가 직접 저장하지 않습니다.
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">2. 개인정보의 수집 및 이용 목적</h2>
            <ul className="list-disc list-inside text-gray-700 space-y-2">
              <li>상품 주문 및 배송 처리</li>
              <li>결제 및 환불 처리</li>
              <li>고객 문의 대응</li>
              <li>서비스 이용 관련 고지사항 전달</li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">3. 개인정보의 보유 및 이용 기간</h2>
            <div className="text-gray-700 space-y-3">
              <p>원칙적으로 개인정보 수집 및 이용 목적이 달성된 후 지체 없이 파기합니다.</p>
              <p>단, 관계 법령에 따라 일정 기간 보관할 수 있습니다.</p>
            </div>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">4. 개인정보의 위탁</h2>
            <p className="text-gray-700 mb-3">회사는 원활한 서비스 제공을 위해 아래와 같이 개인정보 처리를 위탁할 수 있습니다.</p>
            <ul className="list-disc list-inside text-gray-700 space-y-2">
              <li>
                <strong>전자결제 대행:</strong> 토스페이먼츠 (결제 처리)
              </li>
              <li>
                <strong>배송 대행:</strong> 택배사 (상품 배송)
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">5. 개인정보의 파기 절차 및 방법</h2>
            <ul className="list-disc list-inside text-gray-700 space-y-2">
              <li>
                <strong>전자적 파일:</strong> 복구 불가능한 방법으로 삭제
              </li>
              <li>
                <strong>종이 문서:</strong> 분쇄 또는 소각
              </li>
            </ul>
          </section>

          <section>
            <h2 className="text-2xl font-bold mb-4">6. 이용자의 권리</h2>
            <p className="text-gray-700">
              이용자는 언제든지 자신의 개인정보에 대해 열람, 수정, 삭제를 요청할 수 있습니다.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
