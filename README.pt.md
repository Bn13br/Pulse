<div align="center">

# Pulse

**Um visualizador de áudio leve e vibração nos graves para Android**

[English](README.md) · [Português](README.pt.md)

</div>

---

### Introdução

O Pulse traz o visualizador de música *Pulse* das ROMs EvolutionX para qualquer Android, como um app independente. Ele desenha barras animadas na tela enquanto a música toca e também pode transformar o grave da música em vibração, como um pequeno bass shaker.

Ele escuta a **saída global de áudio** pela API `Visualizer` do Android, então funciona com qualquer app de música ou vídeo. Roda como um Serviço de Acessibilidade, ou seja, sem notificação fixa e com custo quase zero quando nada está tocando.

> [!NOTE]
> O Pulse é um projeto independente, inspirado no recurso Pulse do EvolutionX. Não tem ligação com o EvolutionX.

---

### Recursos

**Visualizador**

*   Barras desenhadas sobre qualquer app, na base ou no topo da tela.
*   Quantidade de barras, altura, sensibilidade, opacidade e espaço ajustáveis.
*   Estilos: sólido ou degradê (fade), espelhado a partir do centro e picos que caem devagar.
*   Cores: cor do tema (Monet), cor personalizada em hex ou arco-íris.
*   Exibição opcional na tela de bloqueio.
*   Limite da taxa de captura para economizar bateria.
*   Botão para restaurar o visual padrão.

**Vibração nos graves**

*   Converte a energia dos graves (não o volume geral) em intensidade da vibração, usando análise espectral.
*   Modos: Subgrave, Grave, Batida e Personalizado (sua própria faixa de frequência).
*   Controles: sensibilidade, intensidade máxima, threshold, bass boost (só na análise), attack, release, constância, normalização automática e limitador.
*   Vibração opcional com a tela apagada ou ociosa.
*   Botão de teste e medidor de graves ao vivo para diagnóstico.
*   Funciona em aparelhos sem controle de amplitude (usa pulsos de duração diferente).

**Praticidade**

*   Tile nos atalhos rápidos para ligar e desligar o Pulse.
*   Filtro de apps: esconder ou mostrar só nos apps escolhidos.
*   Idiomas: Português e English, selecionáveis dentro do app.

---

### Compatibilidade

Desenvolvido para **Android 16**. Outras versões não foram testadas.

> [!TIP]
> Root **não é necessário**. Ele só é usado por um atalho opcional que concede permissões e ativa o serviço para você.

---

### Instalação

1. Compile o projeto (por exemplo, com o AndroidIDE ou o Gradle) e instale o APK.
2. Abra o Pulse e toque em **Permitir áudio**.
3. Toque em **Ativar o serviço Pulse** e ligue-o nas configurações de Acessibilidade.
4. Toque uma música.

Se o Android bloquear o serviço como "configuração restrita", abra as informações do app, use o menu e escolha **Permitir configurações restritas**.

**Opcional (root):** toque em **Atalho: configurar tudo com root** para conceder o áudio, liberar as configurações restritas, isentar o app das restrições de bateria e ativar o serviço em um só passo.

---

### Como funciona

1. Um `Visualizer` global captura a saída de áudio e devolve uma FFT.
2. As barras mapeiam faixas da FFT (escala logarítmica) para alturas, com suavização de attack/decay.
3. Na vibração, a energia da faixa de frequência escolhida é normalizada, comparada com uma linha de base lenta (para separar batidas de grave sustentado), passa por threshold e curva, é suavizada por um envelope de attack/release, limitada e enviada ao motor de vibração.

Os detalhes do algoritmo e da integração estão em [`HAPTICS.md`](HAPTICS.md).

---

### Limitações

*   Cada faixa da FFT cobre cerca de 47 Hz (1024 amostras a 48 kHz), então subgrave e grave são separados por pesos, sem precisão fina.
*   A taxa de captura costuma ser de cerca de 20 Hz.
*   As barras não podem ser desenhadas no modo ocioso (always-on / ambient), porque apps não conseguem desenhar ali.
*   A vibração em segundo plano depende da versão do Android e da ROM. Se o feedback tátil do sistema estiver desligado, a vibração pode ser ignorada.
*   O áudio processado por DSPs (como ViPER4Android ou JamesDSP) costuma ser capturado já processado, mas isso depende do aparelho. Use o medidor de graves para conferir.

---

### Privacidade

*   Sem permissão de internet.
*   O serviço lê apenas o **nome do pacote do app na tela**, para aplicar o filtro de apps. Ele não lê o conteúdo da tela.

---

### Solução de problemas

*   Barras paradas: confira se a permissão de áudio foi concedida e se o serviço está ativo.
*   Sem vibração: toque em **Testar vibração** e confira as configurações de vibração e feedback tátil do sistema.
*   Precisa de logs: `su -c "logcat -d | grep -i pulse"`

---

### Créditos

*   [EvolutionX](https://evolution-x.org/): inspiração para o recurso Pulse.
*   API `Visualizer` do Android: captura de áudio e FFT.
