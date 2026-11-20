# Vibração nos graves (BassHaptics)

## Estrutura / integração
Arquivos do módulo: `BassHaptics.java` + as chaves `H_*` e `HAPTIC` de `Prefs.java`.
Para usar em outro projeto (app, módulo, SystemUI de ROM):
1. Copie `BassHaptics.java` e as chaves/defaults de `Prefs.java` (ou troque `configure()` pelos seus próprios valores).
2. Permissões: `VIBRATE`, `RECORD_AUDIO` (necessária para o Visualizer global), `MODIFY_AUDIO_SETTINGS`.
3. Crie `Visualizer(0)` (mix global), `setCaptureSize(1024)`, `setDataCaptureListener(l, rate, false, true)`.
4. No callback de FFT: `bass.process(fft, samplingRateMilliHz, SystemClock.uptimeMillis())`.
5. Chame `bass.configure(prefs)` ao mudar configurações e `bass.reset()` ao parar o áudio/tela.
6. Hospedagem em segundo plano: aqui é o serviço de acessibilidade (sem notificação, o sistema mantém vivo).
   Em outro projeto use um Foreground Service (com notificação) ou rode dentro de um processo de sistema (ROM).

## Algoritmo (por frame de FFT, ~20/s)
1. **Energia na banda**: soma de |bin|² dos bins da FFT que caem em [fmin, fmax], cada bin ponderado
   pela fração do seu intervalo que está dentro da banda. Bin 0 (DC) é ignorado. Raiz da soma = amplitude.
2. **Bass boost (só análise)**: multiplica a amplitude medida (dB → linear). Não altera o som.
3. **Normalização automática**: referência de pico com ataque rápido e queda lenta (~4 s), com piso de
   ruído. nível = amplitude / referência. Assim a vibração se adapta a músicas baixas e altas.
4. **Sensibilidade**: nível × sensibilidade, limitado a 1.
5. **Nível vs. punch**: uma linha de base lenta (~0,9 s) acompanha o nível. `punch = x − base` é o
   quanto o grave subiu de repente (batida). Grave constante perde ~40% da força (não fica "zumbindo").
   - Subgrave/Grave/Personalizado: `d = 0,9·nível + 0,6·punch`
   - Batida: `d = 2,5·punch + 0,2·nível`
6. **Threshold + curva**: abaixo do threshold = 0; acima, reescala para 0..1 e aplica potência 1,5
   (variações pequenas ficam quase imperceptíveis; batidas fortes ficam fortes).
7. **Envelope follower**: sobe com *attack* e desce com *release* (constantes de tempo em ms),
   usando o intervalo real entre frames.
8. **Limiter**: se o envelope ficar acima de 85% por muito tempo, o ganho cai até −50% (volta ao sair).
9. **Intensidade máxima**: escala final. Abaixo de 3% o motor é desligado (cancel).
10. **Motor**: com controle de amplitude → `createOneShot(~70 ms, amplitude 24..255)` renovado a cada frame.
    Sem controle de amplitude → pulsos de duração diferente (12/25/45/70 ms), no máximo ~18/s.

11. **Constância** (0..100%): reduz o corte do grave sustentado no passo 5 e, enquanto o grave
    esteve presente nos últimos 100..500 ms, mantém uma intensidade mínima de até 30%.
    0% = comportamento anterior.

## Limitações
- O Visualizer entrega no máximo 1024 amostras por captura: cada bin da FFT tem ≈47 Hz (a 48 kHz).
  Por isso 20–60 Hz e 60–120 Hz são separados por pesos nos bins 1–2, não com precisão de 1 Hz.
- Taxa máxima de captura costuma ser ~20 Hz (resposta de ~50 ms).
- `AudioPlaybackCapture` não foi usado: exige MediaProjection (diálogo) e não garante pegar áudio
  depois dos efeitos.
- Vibração em segundo plano depende do Android/ROM; aqui usa `USAGE_ASSISTANCE_ACCESSIBILITY`. Se o
  "feedback tátil/vibração" do sistema estiver desligado, o Android pode ignorar.
- DSPs (ViPER4Android, JamesDSP): o Visualizer fica no fim da cadeia de efeitos do mix, então em geral
  enxerga o áudio já processado. Use o medidor da tela para confirmar no seu aparelho.
- Tela apagada/ociosa: a opção `H_SCREEN_OFF` mantém o Visualizer rodando só para a vibração (sem barras).
  Gasta mais bateria e, no Doze profundo, o Android pode suspender a captura.
