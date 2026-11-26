# Android 13 (API 33) como versão mínima

Os Estilos visuais e as artes da Biblioteca usam shaders AGSL (`RuntimeShader`), que existem só a partir do Android 13. O Véu usa o blur nativo, que existe a partir do Android 12. Escolhemos `minSdk = 33` para ter uma única implementação de cada efeito, sem uma versão reduzida para aparelhos antigos. Com isso, aparelhos com Android 12 ou anterior ficam de fora.
