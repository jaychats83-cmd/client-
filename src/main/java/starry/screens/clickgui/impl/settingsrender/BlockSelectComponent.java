package starry.screens.clickgui.impl.settingsrender;

import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;
import starry.modules.module.setting.implement.BlockSelectSetting;
import starry.util.interfaces.AbstractSettingComponent;
import starry.util.render.Render2D;
import starry.util.render.font.Fonts;
import starry.util.render.shader.Scissor;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public class BlockSelectComponent extends AbstractSettingComponent {
    private static final float ROW_HEIGHT = 18f;
    private static final int VISIBLE_ROWS = 8;
    private final BlockSelectSetting setting;
    private boolean expanded, focused;
    private String search = "";
    private int scroll;

    public BlockSelectComponent(BlockSelectSetting setting) { super(setting); this.setting = setting; }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Fonts.BOLD.draw(setting.getName(), x + 1, y + 3, 6, applyAlpha(new Color(220,220,225,220)).getRGB());
        float boxX=x+width-112, boxY=y+1;
        Render2D.rect(boxX,boxY,110,12,applyAlpha(new Color(35,35,40,220)).getRGB(),3);
        Render2D.outline(boxX,boxY,110,12,.5f,applyAlpha(new Color(120,120,130,150)).getRGB(),3);
        String summary = expanded ? (search.isEmpty() ? "Search every block..." : search) : setting.getSelected().size()+" blocks selected";
        Fonts.BOLD.draw(summary,boxX+4,boxY+3.5f,5,applyAlpha(new Color(185,185,195,210)).getRGB());
        if (!expanded) return;
        List<Block> filtered=filtered();
        int maxScroll=Math.max(0,filtered.size()-VISIBLE_ROWS); scroll=Math.min(scroll,maxScroll);
        float listY=boxY+14, listH=Math.min(VISIBLE_ROWS,filtered.size())*ROW_HEIGHT;
        Render2D.rect(boxX,listY,110,listH,applyAlpha(new Color(24,24,28,240)).getRGB(),3);
        Scissor.enable(boxX,listY,110,listH,2);
        for(int row=0;row<Math.min(VISIBLE_ROWS,filtered.size());row++){
            Block block=filtered.get(row+scroll); float ry=listY+row*ROW_HEIGHT;
            boolean hover=mouseX>=boxX&&mouseX<=boxX+110&&mouseY>=ry&&mouseY<ry+ROW_HEIGHT;
            if(hover) Render2D.rect(boxX+1,ry+1,108,ROW_HEIGHT-2,applyAlpha(new Color(80,80,90,90)).getRGB(),2);
            context.drawItem(block.asItem().getDefaultStack(),(int)boxX+3,(int)ry+1);
            String id=Registries.BLOCK.getId(block).toString().replace("minecraft:","");
            if(id.length()>19) id=id.substring(0,18)+"…";
            int color=setting.isSelected(block)?new Color(115,220,145,255).getRGB():new Color(205,205,210,255).getRGB();
            Fonts.BOLD.draw(id,boxX+22,ry+6,5,applyAlpha(color));
        }
        Scissor.disable();
    }

    private List<Block> filtered(){ String q=search.toLowerCase(Locale.ROOT).replace(' ','_'); return setting.getBlocks().stream().filter(b->Registries.BLOCK.getId(b).toString().contains(q)).toList(); }
    @Override public boolean mouseClicked(double mx,double my,int button){
        float boxX=x+width-112,boxY=y+1;
        if(button==0&&mx>=boxX&&mx<=boxX+110&&my>=boxY&&my<=boxY+12){expanded=!expanded;focused=expanded;TextComponent.typing=focused;return true;}
        if(expanded&&button==0){float listY=boxY+14;int row=(int)((my-listY)/ROW_HEIGHT);List<Block> f=filtered();if(mx>=boxX&&mx<=boxX+110&&row>=0&&row<VISIBLE_ROWS&&row+scroll<f.size()){setting.toggle(f.get(row+scroll));return true;}}
        if(expanded){expanded=false;focused=false;TextComponent.typing=false;} return false;
    }
    @Override public boolean mouseScrolled(double mx,double my,double amount){if(!expanded)return false;int max=Math.max(0,filtered().size()-VISIBLE_ROWS);scroll=Math.max(0,Math.min(max,scroll+(amount<0?1:-1)));return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(!focused)return false;if(key==GLFW.GLFW_KEY_ESCAPE){expanded=false;focused=false;TextComponent.typing=false;return true;}if(key==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);scroll=0;return true;}return false;}
    @Override public boolean charTyped(char chr,int modifiers){if(!focused||Character.isISOControl(chr))return false;search+=chr;scroll=0;return true;}
    @Override public boolean isHover(double mx,double my){return mx>=x&&mx<=x+width&&my>=y&&my<=y+getTotalHeight();}
    public float getTotalHeight(){return height+(expanded?Math.min(VISIBLE_ROWS,filtered().size())*ROW_HEIGHT+14:0);}
}
