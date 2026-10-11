# Ultra Capsule Adventure (모드ID `partnercap`) — MC 26.3 Fabric

마인크래프트 자바 **26.3 Wilderness Bound** 대응 Fabric 모드.
- Fabric Loader `0.19.5`, Fabric API `0.161.0+26.3`, Loom `1.18-SNAPSHOT`, Gradle `9.7.1`, Java `25`

위치: `E:\user\개인 작업\마크 모드 제작소\ultra capsule adventure`

## 캡슐 일람

| 캡슐 | 입수 | 용도 |
|---|---|---|
| 파트너 캡슐 | 조합(철괴 4 + 엔더진주 1) | 길들인 몹 보관/소환/관리 |
| 소울 캡슐 | 조합 | 불길의 검 처치 영혼 저장 → 아군 소울 소환 (최대 Lv.3 합성) |
| 도박 캡슐 | 조합 | 아이템 장전 → 10초 후 50%로 2배/소멸 |
| 분해 캡슐 | 조합 | 아이템 역조합 분해 |
| 공허 캡슐 | 조합 | 엔티티 흡수 → 풀어주기/존재 삭제(피해·자식·거래·부활 되돌림) |
| 메카 캡슐 | 조합 불가, 도구제작자 마스터 거래(에메랄드 45, 확률) | 3초 충전 → 탑승형 로봇 변신 |
| 타이탄 캡슐 | 조합(철블록 2 + 다이아 2 + 자수정 1) | 장비 수리 + 손상된 모루 복원 + 메두사 머리 충전 |

## C키 관리창

캡슐을 손에 들고 `C` → 해당 캡슐 탭으로 바로 열림. 상단 버튼으로 탭 순환:
파트너 → 영혼 → 공허 → 타이탄 → 도박 → 분해 → 메카. 각 탭에서 상태 확인 + 버튼 조작.

## 주요 사용법

- **파트너**: 길들인 몹 우클릭 → 반경 64m 내 내 길들임 전부 저장. 허공/블록 우클릭 → 소환. 관리창: 체력바·이름·회복(클릭)/풀회복(0.6초 꾹)·안장/갑옷 장착·불러내기(512m 순간이동)
- **소울**: 불길의 검으로 처치 → 자동 흡수 저장. 같은 종류·레벨 2마리 합성 → Lv+1 (1.5배/2.0배). 우클릭 소환, 웅크리기 우클릭/소울 우클릭 회수. 사망 시 자동 회수 + 리스폰 후 자동 소환. 내 소울끼리 오폭 없음
- **공허**: 엔티티 우클릭 흡수. 존재 삭제 시 2시간 이내 피해·자식·거래·부활·드롭·액체 되돌림. 먹보 도깨비 사망 시 검은 액체(공허 캡슐 재료) 산포
- **메카**: 들고 우클릭 3초 충전 → 캡슐이 날아가 착지 지점에서 변신(포드 진동→부품 전개). 우클릭 탑승 → WASD+스프린트, 좌클릭 레이저(150뎀·64칸), 스페이스 점프. 쉬프트 하차 → 웅크리기 우클릭 유지 → 접힘 후 캡슐이 날아와 인벤토리로. 체력 200, 낙뎀 없음
- **타이탄**: 다른 손 수리품 + 우클릭 장전 → 시간 경과 후 자동 완성·반환 → 5분 쿨타임. 시간 = 등급(최대내구/16 + 인챈트×8) × 손상도 (5초~240초). 모루 우클릭 → 90초 후 멀쩡한 모루로 복원(방향 유지). 메두사 머리 충전 75초. C창에서도 맡기기/꺼내기
- **메두사**: 비 오는 날 플레이어 주변 24~40m에 40% 확률 생성(30초 판정, 근처 2마리 상한). 시선 마주침(31° 이내·16m·시야) 즉시 석화 + 눈 섬광. 근접은 독 공격. 사망 시 머리 100% 드롭
- **석화**: 원본 그 자리 동결(자세·모델 그대로, 0.5초 다리 정리). 무적·무AI·무중력·고정, 타겟팅 제외, 곡괭이(돌 캘 수 있는 도구) 왼클릭만 허용 → 조약돌 2개. 메두사 삭제로 지우면 그 메두사가 돌린 애들 해제. 플레이어도 석화+사망(석상은 돌 인간형)
- **메두사의 머리**: 충전 후 조준 우클릭 → 32m 원거리 석화 1회 → 재수리 필요. 크리에이티브는 무한 사용. 살아있는 머리=아이템 동일 외형, 충전은 반짝임으로 구분

## 빌드 / 실행

```bat
cd "E:\user\개인 작업\마크 모드 제작소\ultra capsule adventure"
.\gradlew.bat runClient   # 테스트 (자동 리빌드됨)
.\gradlew.bat build       # 배포 JAR: build\libs\
```

- 게임 실행 중에는 클래스 교체가 안 됨. 수정 후에는 게임을 완전히 끄고 다시 실행
- `runServer` 첫 실행 시 `run/eula.txt`를 `eula=true`로 변경
- `runServer`/`runClient` 중복 실행 시 포트·파일 잠금 에러 → 기존 프로세스 종료 후 1개만 실행

## 26.3 포팅 메모

- Yarn → Mojang 공식 매핑: `Identifier.fromNamespaceAndPath`, `BuiltInRegistries`, `Item.Properties().setId(key)`
- NBT는 `DataComponents.CUSTOM_DATA` (`CustomData.of/copyTag`). `CompoundTag` getter는 `Optional` 반환 (`orElse` 처리)
- `InteractionResult` Holder 삭제, `Item.TooltipContext`, `level.isClientSide()`, `TagValueOutput`/`loadEntityRecursive`
- 네트워크: `CustomPacketPayload + StreamCodec + PayloadTypeRegistry.serverboundPlay()`
- 아이템 사용: `UseAnim` → `ItemUseAnimation`, `releaseUsing`은 `boolean` 반환
- 사운드: `SoundEvents`에서 `BLOCK_`/`ENTITY_` 접두사 제거 (`PISTON_EXTEND`, `WARDEN_SONIC_BOOM` 등, 일부는 Holder라 `.value()`)
- `PickaxeItem` 클래스 삭제됨 → 곡괭이 판정은 `stack.isCorrectToolForDrops(Blocks.COBBLESTONE...)`
- 주민 거래 API 삭제됨 → 거래는 데이터 드리븐 (`data/*/villager_trade/**` + 태그 머지)
- 엔티티 저장: `ValueOutput(ValueOutput.put*/ValueInput.get*Or)` / `TagValueOutput`
- 석화 렌더는 믹스인 사용 (`partnercap.mixins.json`, `partnercap.client.mixins.json`)
  - `LivingEntity`: 석화 플래그 + tick 동결 + NBT 저장
  - `LivingEntityRenderer`: 상태 플래그 + 나이 동결 + 걷기 easing + 돌 RenderType
  - `RenderTypes`: 몸통·갑옷·발광 텍스처를 돌로 교체
- 컴파일 에러는 javap으로 실제 시그니처 확인 후 수정 (스냅샷마다 메서드명·시그니처가 바뀜)
