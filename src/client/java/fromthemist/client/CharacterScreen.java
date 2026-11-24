package fromthemist.client;

import fromthemist.rpg.Progress;
import fromthemist.rpg.RpgPayloads;
import fromthemist.rpg.Skill;
import fromthemist.rpg.Talent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Level and experience across the top, the three talent trees as columns of buttons (hover for what a talent
 * does, click to put a point in), and the use-based skills along the bottom.
 */
public class CharacterScreen extends Screen {
	private static final int COLUMN = 132;
	private static final int GAP = 8;
	private static final int ROW = 21;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int DIM = 0xFFA0A6AC;
	private static final int GOLD = 0xFFE6B450;
	private static final int BAR_BACK = 0xFF24282C;
	private static final int BAR_XP = 0xFFB8862B;
	private static final int BAR_SKILL = 0xFF3F8F96;
	private int seen = -1;

	public CharacterScreen() {
		super(Component.translatable("screen.fromthemist.character"));
	}

	private int left() {
		return (this.width - (COLUMN * 3 + GAP * 2)) / 2;
	}

	private int top() {
		return Math.max(4, (this.height - 226) / 2);
	}

	@Override
	protected void init() {
		this.seen = RpgClient.version;
		Progress progress = RpgClient.PROGRESS;
		for (Talent talent : Talent.VALUES) {
			int x = this.left() + talent.tree.ordinal() * (COLUMN + GAP);
			int y = this.top() + 52 + talent.position() * ROW;
			int rank = progress.rank(talent);
			Component label = Component.translatable("talent.fromthemist." + talent.key()).append(" " + rank + "/" + talent.maxRank);
			Component description = Component.translatable("talent.fromthemist." + talent.key() + ".desc");
			if (talent.isAbility()) {
				description = description.copy().append("\n").append(Component.translatable("screen.fromthemist.ability_needs", Talent.CAPSTONE_POINTS,
					RpgClient.ABILITY[talent.abilitySlot()].getTranslatedKeyMessage()));
			}

			Button button = Button.builder(label, pressed -> ClientPlayNetworking.send(new RpgPayloads.Spend(talent.ordinal())))
				.bounds(x, y, COLUMN, 20)
				.tooltip(Tooltip.create(description))
				.build();
			button.active = progress.canSpend(talent);
			this.addRenderableWidget(button);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.seen != RpgClient.version) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Progress progress = RpgClient.PROGRESS;
		int left = this.left();
		int top = this.top();
		int right = left + COLUMN * 3 + GAP * 2;

		graphics.centeredText(this.font, this.title, this.width / 2, top, WHITE);
		boolean capped = progress.level >= Progress.MAX_LEVEL;
		int needed = Progress.needed(progress.level);
		graphics.text(this.font, Component.translatable("screen.fromthemist.level", progress.level), left, top + 12, GOLD);
		Component points = Component.translatable("screen.fromthemist.points", progress.points);
		graphics.text(this.font, points, right - this.font.width(points), top + 12, progress.points > 0 ? GOLD : DIM);
		bar(graphics, left, top + 23, right - left, capped ? 1.0F : progress.xp / (float) needed, BAR_XP);
		graphics.centeredText(this.font, capped ? Component.translatable("screen.fromthemist.max") : Component.literal(progress.xp + " / " + needed), this.width / 2, top + 30, DIM);

		for (Talent.Tree tree : Talent.Tree.values()) {
			int x = left + tree.ordinal() * (COLUMN + GAP);
			graphics.centeredText(this.font, Component.translatable("tree.fromthemist." + tree.name().toLowerCase(java.util.Locale.ROOT)), x + COLUMN / 2, top + 42, WHITE);
			int cooldown = RpgClient.COOLDOWNS[tree.ordinal()];
			if (cooldown > 0 && progress.rank(Talent.ability(tree.ordinal())) > 0) {
				graphics.centeredText(this.font, Component.translatable("screen.fromthemist.cooldown", cooldown / 20 + 1), x + COLUMN / 2, top + 52 + Talent.PER_TREE * ROW, DIM);
			}
		}

		int skillsTop = top + 52 + Talent.PER_TREE * ROW + 8;
		graphics.text(this.font, Component.translatable("screen.fromthemist.skills"), left, skillsTop, WHITE);
		for (Skill skill : Skill.VALUES) {
			int y = skillsTop + 11 + skill.ordinal() * 10;
			int level = progress.skillLevels[skill.ordinal()];
			graphics.text(this.font, Component.translatable("skill.fromthemist." + skill.key()), left, y, DIM);
			graphics.text(this.font, String.valueOf(level), left + 62, y, WHITE);
			float filled = level >= Skill.MAX_LEVEL ? 1.0F : progress.skillXp[skill.ordinal()] / (float) Skill.needed(level);
			bar(graphics, left + 80, y + 2, 110, filled, BAR_SKILL);
			graphics.text(this.font, Component.translatable("skill.fromthemist." + skill.key() + ".bonus", bonus(skill, level)), left + 198, y, DIM);
		}
	}

	private static String bonus(final Skill skill, final int level) {
		double each = switch (skill) {
			case ATHLETICS -> 0.6;
			case ENDURANCE -> 1.0;
			default -> 2.0;
		};
		double total = each * level;
		return total == Math.floor(total) ? String.valueOf((int) total) : String.format(java.util.Locale.ROOT, "%.1f", total);
	}

	private static void bar(final GuiGraphicsExtractor graphics, final int x, final int y, final int width, final float filled, final int colour) {
		graphics.fill(x, y, x + width, y + 5, BAR_BACK);
		graphics.fill(x, y, x + Math.round(width * Math.clamp(filled, 0.0F, 1.0F)), y + 5, colour);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
