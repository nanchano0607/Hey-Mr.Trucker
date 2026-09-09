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

          <section>
            <h2 className="text-2xl font-bold mb-4">7. 쿠키 및 온라인 맞춤형 광고</h2>
            <p className="text-gray-700 mb-3">
              회사는 이용자에게 최적화된 광고를 제공하고 광고 효과를 측정하기 위해 쿠키(Cookie)
              및 이와 유사한 기술을 사용할 수 있습니다.
            </p>

            <h3 className="text-xl font-semibold mb-2">가. 자동 수집 항목</h3>
            <ul className="list-disc list-inside text-gray-700 space-y-1 mb-4">
              <li>방문 및 서비스 이용 기록(방문 일시, 조회 페이지 등)</li>
              <li>쿠키 식별자, 기기 및 브라우저 정보, IP 주소</li>
            </ul>

            <h3 className="text-xl font-semibold mb-2">나. 수집 목적</h3>
            <ul className="list-disc list-inside text-gray-700 space-y-1 mb-4">
              <li>맞춤형 광고 제공 및 광고 성과 측정</li>
              <li>서비스 이용 통계 분석</li>
            </ul>

            <h3 className="text-xl font-semibold mb-2">다. 제공받는 자 및 국외 이전</h3>
            <p className="text-gray-700 mb-3">
              회사는 온라인 맞춤형 광고 제공을 위해 쿠키를 통해 수집된 온라인 행태정보를 아래와
              같이 국외의 제3자에게 제공(이전)합니다. 이용자는 이에 대한 동의를 거부할 권리가
              있으며, 거부하더라도 서비스 이용에는 제한이 없습니다.
            </p>
            <div className="overflow-x-auto mb-4">
              <table className="w-full text-sm text-left text-gray-700 border border-gray-300">
                <tbody>
                  <tr className="border-b border-gray-300">
                    <th className="px-3 py-2 bg-gray-300 font-semibold w-40">이전받는 자</th>
                    <td className="px-3 py-2">Meta Platforms, Inc. (메타)</td>
                  </tr>
                  <tr className="border-b border-gray-300">
                    <th className="px-3 py-2 bg-gray-300 font-semibold">이전되는 국가</th>
                    <td className="px-3 py-2">미국</td>
                  </tr>
                  <tr className="border-b border-gray-300">
                    <th className="px-3 py-2 bg-gray-300 font-semibold">이전 일시 및 방법</th>
                    <td className="px-3 py-2">쿠키 동의 시 네트워크를 통한 실시간 전송</td>
                  </tr>
                  <tr className="border-b border-gray-300">
                    <th className="px-3 py-2 bg-gray-300 font-semibold">이전 항목</th>
                    <td className="px-3 py-2">쿠키 식별자, 방문·이용 기록, IP 주소, 기기 정보</td>
                  </tr>
                  <tr className="border-b border-gray-300">
                    <th className="px-3 py-2 bg-gray-300 font-semibold">이용 목적</th>
                    <td className="px-3 py-2">광고 타겟팅 및 광고 성과 측정</td>
                  </tr>
                  <tr>
                    <th className="px-3 py-2 bg-gray-300 font-semibold">보유 및 이용 기간</th>
                    <td className="px-3 py-2">
                      Meta의 개인정보처리방침에 따름 (동의 철회 또는 목적 달성 시까지)
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <h3 className="text-xl font-semibold mb-2">라. 동의 거부 방법</h3>
            <ul className="list-disc list-inside text-gray-700 space-y-1">
              <li>사이트 방문 시 노출되는 쿠키 동의 배너에서 "거부"를 선택하면 광고 목적의 쿠키가 저장되지 않습니다.</li>
              <li>웹 브라우저 설정에서 쿠키 저장을 차단하거나 삭제할 수 있습니다. (단, 쿠키 저장을 거부할 경우 일부 서비스 이용에 어려움이 있을 수 있습니다.)</li>
              <li>
                메타 광고 설정 페이지(facebook.com/adpreferences/ad_settings)에서 맞춤형 광고 수신을
                별도로 거부할 수 있습니다.
              </li>
            </ul>
          </section>
        </div>
      </div>
    </div>
  );
}
