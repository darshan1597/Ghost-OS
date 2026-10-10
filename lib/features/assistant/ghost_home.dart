import 'package:flutter/material.dart';

const neonGradient = LinearGradient(
  colors: [Color(0xFF00DFFF), Color(0xFF167AFF), Color(0xFFB42AFF)],
);

class GradientText extends StatelessWidget {
  const GradientText(this.text, {super.key, required this.style});
  final String text;
  final TextStyle style;
  @override
  Widget build(BuildContext context) => ShaderMask(
    blendMode: BlendMode.srcIn,
    shaderCallback: (bounds) => neonGradient.createShader(bounds),
    child: Text(text, style: style.copyWith(color: Colors.white)),
  );
}

class GhostHome extends StatelessWidget {
  const GhostHome({
    super.key,
    required this.name,
    required this.state,
    required this.command,
    required this.busy,
    required this.onSubmit,
    required this.onMicrophone,
    required this.onListening,
    required this.onQuickCommand,
  });
  final String name;
  final Map<String, dynamic> state;
  final TextEditingController command;
  final bool busy;
  final VoidCallback onSubmit, onMicrophone, onListening;
  final ValueChanged<String> onQuickCommand;

  @override
  Widget build(BuildContext context) {
    final listening = state['listening'] == true;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: 18),
        const Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'Ghost',
              style: TextStyle(
                fontSize: 37,
                fontWeight: FontWeight.w700,
                letterSpacing: -1.5,
                color: Colors.white,
              ),
            ),
            GradientText(
              'OS',
              style: TextStyle(
                fontSize: 37,
                fontWeight: FontWeight.w700,
                letterSpacing: -1.5,
              ),
            ),
          ],
        ),
        const SizedBox(height: 3),
        const Text(
          'YOUR AI. YOUR PHONE.',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 10,
            letterSpacing: 3,
            color: Color(0xFFB6C0DA),
          ),
        ),
        const SizedBox(height: 20),
        Semantics(
          label: '$name assistant',
          image: true,
          child: Transform.scale(
            scale: 1.3,
            child: Image.asset(
              'assets/branding/ghost_hero.png',
              height: 222,
              fit: BoxFit.contain,
              excludeFromSemantics: true,
            ),
          ),
        ),
        const SizedBox(height: 2),
        Semantics(
          label: 'Hey, I’m $name',
          container: true,
          child: ExcludeSemantics(
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    'Hey, I’m ',
                    style: TextStyle(
                      fontSize: 30,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                  GradientText(
                    name,
                    style: const TextStyle(
                      fontSize: 30,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(height: 7),
        const Text(
          'Just tell me what you need,\nand I’ll take care of it.',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 15,
            height: 1.35,
            color: Color(0xFFA9B7D0),
          ),
        ),
        const SizedBox(height: 17),
        Container(
          padding: const EdgeInsets.fromLTRB(10, 5, 8, 5),
          decoration: BoxDecoration(
            color: const Color(0xAA0C1427),
            borderRadius: BorderRadius.circular(25),
            border: Border.all(color: const Color(0xFF263C62)),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(9),
                decoration: BoxDecoration(
                  color: const Color(0xFF142139),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Icon(
                  Icons.chat_bubble_outline_rounded,
                  color: Color(0xFFC9D9F6),
                  size: 22,
                ),
              ),
              const SizedBox(width: 3),
              Expanded(
                child: TextField(
                  controller: command,
                  maxLength: 500,
                  textInputAction: TextInputAction.send,
                  onSubmitted: (_) => onSubmit(),
                  style: const TextStyle(fontSize: 13, color: Colors.white),
                  decoration: const InputDecoration(
                    hintText: 'Type your command here...',
                    counterText: '',
                    filled: false,
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                    contentPadding: EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 12,
                    ),
                  ),
                ),
              ),
              if (command.text.isNotEmpty)
                IconButton(
                  tooltip: 'Run command',
                  onPressed: busy ? null : onSubmit,
                  icon: const Icon(Icons.arrow_upward, color: Colors.white),
                ),
            ],
          ),
        ),
        const SizedBox(height: 13),
        _MicrophoneControl(
          assistantName: name,
          listening: listening,
          starting:
              listening &&
              (state['status'] as String? ?? '').toLowerCase().contains(
                'loading',
              ),
          busy: busy,
          onListening: onListening,
          onMicrophone: onMicrophone,
        ),
        if (listening || state['speaking'] == true) ...[
          const SizedBox(height: 8),
          Text(
            state['speaking'] == true
                ? 'My turn to speak. Yours right after, boss.'
                : state['status'] as String? ?? 'Listening',
            textAlign: TextAlign.center,
            style: const TextStyle(color: Color(0xFFA9B7D0), fontSize: 11),
          ),
        ],
        const SizedBox(height: 20),
        const Text(
          'Try these commands',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: Color(0xFFBCCAE0),
          ),
        ),
        const SizedBox(height: 9),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: _CommandCard(
                icon: Icons.chat,
                color: const Color(0xFF00D267),
                text: 'Open WhatsApp\nor message someone in your contacts',
                onTap: busy ? null : () => onQuickCommand('Open WhatsApp'),
              ),
            ),
            const SizedBox(width: 9),
            Expanded(
              child: _CommandCard(
                icon: Icons.calendar_month,
                color: const Color(0xFFFF9200),
                text: 'Ask me to set\nan alarm for the time you need',
                onTap: busy ? null : () => onQuickCommand('Set an alarm'),
              ),
            ),
            const SizedBox(width: 9),
            Expanded(
              child: _CommandCard(
                icon: Icons.mail_outline,
                color: const Color(0xFF008FFF),
                text: 'Ask me to open Gmail on your phone',
                onTap: busy ? null : () => onQuickCommand('Open Gmail'),
              ),
            ),
          ],
        ),
        if ((state['transcript'] as String? ?? '').isNotEmpty) ...[
          const SizedBox(height: 15),
          Text(
            'Heard: ${state['transcript']}',
            style: const TextStyle(color: Colors.white60, fontSize: 12),
          ),
        ],
        if ((state['history'] as List? ?? []).isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(
            (state['history'] as List).first['outcome'] as String,
            style: const TextStyle(color: Color(0xFF65DFFF), height: 1.4),
          ),
        ],
        const SizedBox(height: 12),
      ],
    );
  }
}

class _CommandCard extends StatelessWidget {
  const _CommandCard({
    required this.icon,
    required this.color,
    required this.text,
    required this.onTap,
  });
  final IconData icon;
  final Color color;
  final String text;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => Material(
    color: Colors.transparent,
    child: Ink(
      decoration: BoxDecoration(
        color: const Color(0xC0091421),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF20334B)),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Padding(
          padding: const EdgeInsets.all(11),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(5),
                    decoration: BoxDecoration(
                      color: color,
                      borderRadius: BorderRadius.circular(9),
                    ),
                    child: Icon(icon, size: 23, color: Colors.white),
                  ),
                  const Spacer(),
                  const Icon(
                    Icons.chevron_right,
                    color: Color(0xFF8091B3),
                    size: 16,
                  ),
                ],
              ),
              const SizedBox(height: 9),
              Text(
                text,
                style: const TextStyle(
                  fontSize: 11,
                  height: 1.4,
                  color: Color(0xFFE3EAF7),
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

class _GlowButton extends StatelessWidget {
  const _GlowButton({
    required this.icon,
    required this.label,
    required this.onTap,
    required this.size,
  });
  final IconData icon;
  final String label;
  final VoidCallback? onTap;
  final double size;
  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xFF00CFFF), Color(0xFF2255FF), Color(0xFF9923FF)],
      ),
      boxShadow: [
        BoxShadow(
          color: const Color(0xFF3E36FF).withValues(alpha: .35),
          blurRadius: 18,
        ),
      ],
    ),
    child: IconButton(
      tooltip: label,
      onPressed: onTap,
      icon: icon == Icons.graphic_eq
          ? GhostWaveform(size: size * .48)
          : Icon(icon, color: Colors.white, size: size * .48),
    ),
  );
}

class GhostNavigation extends StatelessWidget {
  const GhostNavigation({
    super.key,
    required this.selected,
    required this.onSelected,
  });
  final int selected;
  final ValueChanged<int> onSelected;
  @override
  Widget build(BuildContext context) => SafeArea(
    top: false,
    child: CustomPaint(
      painter: _DockPainter(),
      child: SizedBox(
        height: 101,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            Expanded(child: _destination('History', Icons.access_time, 2)),
            Transform.translate(
              offset: const Offset(0, -16),
              child: _GlowButton(
                icon: Icons.graphic_eq,
                label: 'Assistant',
                size: 70,
                onTap: () => onSelected(0),
              ),
            ),
            Expanded(
              child: _destination('Settings', Icons.settings_outlined, 1),
            ),
          ],
        ),
      ),
    ),
  );
  Widget _destination(String label, IconData icon, int index) => InkWell(
    onTap: () => onSelected(index),
    borderRadius: BorderRadius.circular(24),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            size: 25,
            color: selected == index
                ? const Color(0xFF49CDFF)
                : const Color(0xFFA9BAD9),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              color: selected == index ? Colors.white : const Color(0xFFA9BAD9),
              fontSize: 11,
            ),
          ),
          const SizedBox(height: 7),
          Container(
            width: 5,
            height: 5,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: selected == index
                  ? const Color(0xFF00CFFF)
                  : Colors.transparent,
            ),
          ),
        ],
      ),
    ),
  );
}

class _DockPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final path = Path()
      ..moveTo(0, size.height)
      ..lineTo(0, 54)
      ..quadraticBezierTo(0, 11, 42, 11)
      ..lineTo(w * .33, 11)
      ..cubicTo(w * .41, 11, w * .43, -9, w * .5, -9)
      ..cubicTo(w * .57, -9, w * .59, 11, w * .67, 11)
      ..lineTo(w - 42, 11)
      ..quadraticBezierTo(w, 11, w, 54)
      ..lineTo(w, size.height)
      ..close();
    final paint = Paint()
      ..shader = const LinearGradient(
        colors: [Color(0xF0031934), Color(0xF0050B1C), Color(0xF0081433)],
      ).createShader(Offset.zero & size);
    canvas.drawPath(path, paint);
    paint
      ..shader = null
      ..color = const Color(0xFF007EC9)
      ..style = PaintingStyle.stroke
      ..strokeWidth = .7;
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class GhostWaveform extends StatelessWidget {
  const GhostWaveform({super.key, this.size = 28});
  final double size;
  @override
  Widget build(BuildContext context) =>
      CustomPaint(size: Size.square(size), painter: _WaveformPainter());
}

class _WaveformPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white
      ..strokeWidth = size.width * .065
      ..strokeCap = StrokeCap.round;
    const heights = [.22, .6, .95, .6, .22];
    for (var i = 0; i < heights.length; i++) {
      final x = size.width * (.14 + i * .18);
      final half = size.height * heights[i] / 2;
      canvas.drawLine(
        Offset(x, size.height / 2 - half),
        Offset(x, size.height / 2 + half),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

class _MicrophoneControl extends StatelessWidget {
  const _MicrophoneControl({
    required this.assistantName,
    required this.listening,
    required this.starting,
    required this.busy,
    required this.onListening,
    required this.onMicrophone,
  });
  final String assistantName;
  final bool listening, starting, busy;
  final VoidCallback onListening, onMicrophone;
  @override
  Widget build(BuildContext context) {
    final accent = starting
        ? const Color(0xFFFFC460)
        : listening
        ? const Color(0xFF00E5BA)
        : const Color(0xFF8EA3C7);
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          colors: listening
              ? const [Color(0xFF032936), Color(0xFF10162F)]
              : const [Color(0xFF0B1426), Color(0xFF111127)],
        ),
        border: Border.all(color: accent.withValues(alpha: .7)),
        boxShadow: listening
            ? [BoxShadow(color: accent.withValues(alpha: .12), blurRadius: 20)]
            : [],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              if (listening && !starting)
                _GlowButton(
                  icon: Icons.mic_rounded,
                  label: 'Speak now',
                  size: 42,
                  onTap: busy ? null : onMicrophone,
                )
              else
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: accent.withValues(alpha: .12),
                  ),
                  child: starting
                      ? Padding(
                          padding: const EdgeInsets.all(12),
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: accent,
                          ),
                        )
                      : Icon(Icons.mic_off_rounded, color: accent),
                ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      starting
                          ? 'Microphone starting'
                          : listening
                          ? 'Microphone on'
                          : 'Microphone off',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      starting
                          ? 'Loading offline English recognition...'
                          : listening
                          ? 'Listening for "Hey $assistantName"'
                          : 'Voice listening is disabled',
                      style: const TextStyle(
                        color: Color(0xFFAEBFE4),
                        fontSize: 11,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
                decoration: BoxDecoration(
                  color: accent.withValues(alpha: .12),
                  borderRadius: BorderRadius.circular(15),
                ),
                child: Text(
                  starting
                      ? 'STARTING'
                      : listening
                      ? 'ON'
                      : 'OFF',
                  style: TextStyle(
                    color: accent,
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: busy ? null : onListening,
            icon: Icon(
              listening ? Icons.mic_off_rounded : Icons.mic_rounded,
              size: 18,
            ),
            label: Text(listening ? 'Disable microphone' : 'Enable Microphone'),
            style: OutlinedButton.styleFrom(
              foregroundColor: listening
                  ? const Color(0xFFFF8FA7)
                  : const Color(0xFF32CDFF),
              side: BorderSide(
                color: listening
                    ? const Color(0xFF704257)
                    : const Color(0xFF245C99),
              ),
              padding: const EdgeInsets.symmetric(vertical: 10),
            ),
          ),
        ],
      ),
    );
  }
}
