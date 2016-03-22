package nc.ui.mmmrp.zbnd.pub;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import common.Logger;
import nc.bs.framework.common.NCLocator;
import nc.itf.uap.IUAPQueryBS;
import nc.itf.zbneed.zbneedqg;
import nc.jdbc.framework.processor.BeanProcessor;
import nc.jdbc.framework.processor.MapProcessor;
import nc.ui.ls.MessageBox;
import nc.ui.pub.beans.UIButton;
import nc.ui.pub.beans.UIDialog;
import nc.ui.pub.beans.UIPanel;
import nc.ui.pub.bill.BillCardPanel;
import nc.ui.pub.bill.BillModel;
import nc.ui.pubapp.uif2app.model.BillManageModel;
import nc.ui.pubapp.uif2app.view.ShowUpableBillForm;
import nc.ui.trade.business.HYPubBO_Client;
import nc.ui.uif2.ShowStatusBarMsgUtil;
import nc.uif.pub.exception.UifException;
import nc.vo.bd.material.MaterialConvertVO;
import nc.vo.bd.material.MaterialVO;
import nc.vo.bd.material.plan.MaterialPlanVO;
import nc.vo.mmmrp.zbnd.ZbndBVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDouble;
import nc.vo.so.m30.entity.SaleOrderBVO;
import nc.vo.so.m30.entity.SaleOrderHVO;
import nc.vo.mmpps.mps0202.PoVO;

/**
 * 纸板获取dlg
 * 
 * @author Administrator 2015-05-20 16:06:10
 */
public class ZbndhuoquDLG extends UIDialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 450077882702123254L;
	private UIPanel pnl_center = null;// 布局面板
	private UIPanel pnl_south = null;

	private BillManageModel model;
	private ShowUpableBillForm billform;
	private BillCardPanel pnl_down = null;//

	private UIButton btn_ok = null;// 确定
	private UIButton btn_cancel = null;// 取消
	// 其实截止日期
	String qizhitime = "";
	String jiezhitime = "";
	// 坑类和层数
	String klei = "";
	String ceng = "";
	private IUAPQueryBS ce = (IUAPQueryBS) NCLocator.getInstance().lookup(
			IUAPQueryBS.class);

	@SuppressWarnings("deprecation")
	public ZbndhuoquDLG(BillManageModel model, ShowUpableBillForm billform,
			Object qizhitime, Object jiezhitime) {
		this.model = model;
		this.billform = billform;
		this.qizhitime = qizhitime.toString();
		this.jiezhitime = jiezhitime.toString();
		initialize();

	}

	/**
	 * 取消
	 * 
	 * @param e
	 */
	private void onBoCancel(ActionEvent e) {
		this.close();
	}

	/**
	 * 确认
	 * 
	 * @param e
	 * @return
	 * @throws UifException
	 */
	private void onBoOk(ActionEvent e) {
		// 选择的vo
		List<PoVO> listvos = new ArrayList<PoVO>();
		int rowcount = pnl_down.getBillTable().getRowCount();
		for (int i = 0; i < rowcount; i++) {
			int rowstate = pnl_down.getBillModel().getRowState(i);
			if (rowstate == BillModel.SELECTED) {
				PoVO vo = (PoVO) pnl_down.getBillModel().getBodyValueRowVO(i,
						PoVO.class.getName());
				listvos.add(vo);
			}
		}

		// 没有选择值
		if (listvos.size() == 0) {
			MessageBox.showMessageDialog("提示", "未选中数据");
		} else// 有值的处理
		{ // 保存值班需求表的子表vo
			List<ZbndBVO> vector = new Vector<ZbndBVO>();
			PoVO[] pvos1 = new PoVO[listvos.size()];
			// 回写选中的mrp计划订单单据状态为1，即为选中状态
			for (int i = 0; i < listvos.size(); i++) {
				pvos1[i] = listvos.get(i);
				pvos1[i].setVdef18("1");
				pvos1[i].setDr(0);
			}
			// 调用接口，回写
			try {
				PoVO[] sdvos = NCLocator.getInstance().lookup(zbneedqg.class).qgzttoset1(pvos1);
			} catch (BusinessException e1) {
				// TODO 自动生成的 catch 块
				e1.printStackTrace();
			}
			// 轮训查询每个选中的纸板，并分解编码
			for (PoVO poVO : listvos) {
                //销售订单号
				 
				String vbillcode = poVO.getVfirstcode();
				String cmaterialid = poVO.getCmaterialid();
				//来源单据表体主键
				String vfirstbid = poVO.getVfirstbid();
				Boolean flags = false;
				try {
					flags = getTsMaterialId(vfirstbid);
				} catch (BusinessException e3) {
					// TODO 自动生成的 catch 块
					e3.printStackTrace();
				}
				MaterialVO yzwl = new MaterialVO();
                if(flags!=true){
                	try {
    					// 获取原纸物料，取出他的编码
    					yzwl = (MaterialVO) HYPubBO_Client.queryByPrimaryKey(MaterialVO.class, poVO
    									.getCmaterialvid().toString());
    					if (yzwl.getName() == null) {
    						new Exception("没有找到物料");
    					}
    					// 原纸的编码
    					String scz = yzwl.getCode().toString();

    					if (scz.length() > 0) {
    						// 判断第一个字段是否为‘-’
    						String one = scz.substring(0, 1);
    						String str = null;
    						// 基数层
    						if (!one.equals("-")) {
    							try {
    								str = scz.split("-")[0];// 取纸板编码如：2b8b2
    								klei = scz.split("-")[1];// 取坑类
    							} catch (Exception e1) {

    								MessageBox.showMessageDialog("提示", "已选择的物料编码"
    										+ scz + "不规范");
    								return;
    							}

    							if (str != null) {
    								for (int i = 0; i < str.length(); i++) {
    									// 保存与该原纸同样的表体VO，进行叠加
    									ZbndBVO ycbvo = new ZbndBVO();
    									// 判断表体唯一性的字段
    									int shu = 0;
    									// 原纸编码如2-1250
    									String yzkey = str.substring(i, i + 1)
    											+ "-" + poVO.getVdef3().toString();
    									Integer flag = 0;
    									for (int n = 0; n < vector.size(); n++) {
    										// 判断相同的时候才赋值
    										if (vector.get(n).getBdef1()
    												.equals(yzkey)) {
    											shu = n;
    											ycbvo = vector.get(n);
    											// 置是否重复标志位
    											flag++;
    										}
    									}
    									if (flag != 0) {

    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");
    										// 计算原纸需求
    										UFDouble yzkz = new UFDouble(0);
    										UFDouble yzjz = new UFDouble(0);
    										UFDouble yzz = new UFDouble(0);
    										double yzx = 0;

    										if (bvos.length > 0) {
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);
    											// 获取原纸克重
    											//yzkz = new UFDouble(bvos[0].getDef5());
    											yzkz = new UFDouble(bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = Math
    													.ceil(((yzz.doubleValue()) / (yzjz
    															.doubleValue())));

    											ycbvo.setYznumber(ycbvo
    													.getYznumber().add(
    															new UFDouble(yzx)));
    											ycbvo.setNeednumber(ycbvo
    													.getNeednumber().add(yzz));

    											vector.set(shu, ycbvo);
    											flag = 0;
    										}
    									} else {
    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");

    										if (bvos.length > 0) {

    											// 计算原纸需求
    											UFDouble yzkz = new UFDouble(0);
    											UFDouble yzjz = new UFDouble(0);
    											UFDouble yzz = new UFDouble(0);
    											UFDouble yzx = new UFDouble(0);
    											MaterialPlanVO[] mpvo = (MaterialPlanVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialPlanVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "' and pk_org = '"
    																	+ poVO.getPk_org()
    																	+ "'");

    											// 获取原纸克重
    											yzkz = new UFDouble(
    													bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = new UFDouble(yzz.div(yzjz));

    											ZbndBVO bvo = new ZbndBVO();
    											// 设置原纸编码
    											bvo.setCode(bvos[0].getPrimaryKey());
    											// 设置原纸名字
    											bvo.setName(bvos[0].getName());
    											// 设置来源单据类型
    											bvo.setCsrctype("55B4");
    											// 设置提前期
    											bvo.setHeadtime(mpvo[0]
    													.getFixedahead().toString());
    											// 设置组织
    											bvo.setPk_org(poVO.getPk_org());
    											bvo.setPk_org_v(poVO.getPk_org_v());
    											// 设置原纸幅宽
    											bvo.setYzfk(new UFDouble(poVO
    													.getVdef3()));
    											bvo.setYzkz(yzkz);
    											// 设置需求日期为当前日期
    											bvo.setNeedtime(new UFDate());
    											// 获取原纸需求重量
    											bvo.setNeednumber(yzz);
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);

    											// 设置单位
    											MaterialConvertVO[] mcvo = (MaterialConvertVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialConvertVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "'");
    											if (mcvo.length == 0) {
    												MessageBox
    														.showMessageDialog(
    																"提示",
    																"物料编码为"
    																		+ bvos[0]
    																				.getCode()
    																		+ "的辅计量单位表体为空");
    												return;
    											}
    											bvo.setUnit(mcvo[0].getPk_measdoc());
    											bvo.setYznumber(new UFDouble(
    													Math.ceil(((yzz
    															.doubleValue()) / (yzjz
    															.doubleValue())))));
    											bvo.setBdef1(str
    													.substring(i, i + 1)
    													+ "-"
    													+ poVO.getVdef3()
    															.toString());
    											vector.add(bvo);
    										}
    									}

    								}

    							}
    						}

    						// 双层纸的情况，前面有一个‘-’符号。
    						else {
    							try {

    								String yxzd = scz.substring(1, scz.length());
    								str = yxzd.split("-")[0];// 取纸板编码如：2b8b2
    								klei = yxzd.split("-")[1];// 取坑类
    							} catch (Exception e2) {

    								MessageBox.showMessageDialog("提示", "已选择的物料编码"
    										+ scz + "不规范");
    								return;
    							}

    							if (str.length() > 0) {
    								for (int i = 0; i < str.length(); i++) {
    									// 保存与该原纸同样的表体VO，进行叠加
    									ZbndBVO ycbvo = new ZbndBVO();
    									int shu = 0;
    									String yzkey = str.substring(i, i + 1)
    											+ "-" + poVO.getVdef3().toString();
    									Integer flag = 0;
    									for (int n = 0; n < vector.size(); n++) {
    										if (vector.get(n).getBdef1()
    												.equals(yzkey)) {
    											shu = n;
    											ycbvo = vector.get(n);
    											// 置是否重复标志位
    											flag++;
    										}
    									}
    									if (flag != 0) {
    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");
    										// 计算原纸需求
    										UFDouble yzkz = new UFDouble(0);
    										UFDouble yzjz = new UFDouble(0);
    										UFDouble yzz = new UFDouble(0);
    										double yzx = 0;

    										if (bvos.length > 0) {
    											// 获取原纸克重
    											yzkz = new UFDouble(
    													bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = Math
    													.ceil(((yzz.doubleValue()) / (yzjz
    															.doubleValue())));
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);
    											ycbvo.setYznumber(ycbvo
    													.getYznumber().add(
    															new UFDouble(yzx)));
    											ycbvo.setNeednumber(ycbvo
    													.getNeednumber().add(yzz));
    											vector.set(shu, ycbvo);
    										}
    									} else {
    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");

    										if (bvos.length > 0) {
    											// 计算原纸需求
    											UFDouble yzkz = new UFDouble(0);
    											UFDouble yzjz = new UFDouble(0);
    											UFDouble yzz = new UFDouble(0);
    											UFDouble yzx = new UFDouble(0);
    											MaterialPlanVO[] mpvo = (MaterialPlanVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialPlanVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "' and pk_org = '"
    																	+ poVO.getPk_org()
    																	+ "'");

    											// 获取原纸克重
    											yzkz = new UFDouble(
    													bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = new UFDouble(yzz.div(yzjz));

    											ZbndBVO bvo = new ZbndBVO();
    											// 设置原纸编码
    											bvo.setCode(bvos[0].getPrimaryKey());
    											// 设置原纸名字
    											bvo.setName(bvos[0].getName());
    											// 设置来源单据类型
    											bvo.setCsrctype("55B4");
    											// 设置提前期
    											bvo.setHeadtime(mpvo[0]
    													.getFixedahead().toString());
    											// 设置组织
    											bvo.setPk_org(poVO.getPk_org());
    											bvo.setPk_org_v(poVO.getPk_org_v());
    											// 设置原纸幅宽
    											bvo.setYzfk(new UFDouble(poVO
    													.getVdef3()));
    											bvo.setYzkz(yzkz);
    											// 设置需求日期为当前日期
    											bvo.setNeedtime(new UFDate());
    											// 获取原纸需求重量
    											bvo.setNeednumber(yzz);
    											// 设置单位
    											MaterialConvertVO[] mcvo = (MaterialConvertVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialConvertVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "'");
    											bvo.setUnit(mcvo[0].getPk_measdoc());
    											bvo.setYznumber(new UFDouble(
    													Math.ceil(((yzz
    															.doubleValue()) / (yzjz
    															.doubleValue())))));
    											bvo.setBdef1(str
    													.substring(i, i + 1)
    													+ "-"
    													+ poVO.getVdef3()
    															.toString());
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);
    											vector.add(bvo);
    										}
    									}

    								}

    							}

    						}

    					}

    				} catch (UifException e1) {
    					// TODO 自动生成的 catch 块
    					e1.printStackTrace();
    				}
                }else{
                	try {
    					// 获取原纸物料，取出他的编码
    					yzwl = (MaterialVO) HYPubBO_Client.queryByPrimaryKey(MaterialVO.class, poVO
    									.getCmaterialvid().toString());
    					
    					if (yzwl.getName() == null) {
    						new Exception("没有找到物料");
    					}
    					// 原纸的编码
    					String scz = yzwl.getCode().toString();

    					if (scz.length() > 0) {
    						// 判断第一个字段是否为‘-’
    						String one = scz.substring(0, 1);
    						String str = null;
    						// 基数层
    						if (!one.equals("-")) {
    							try {
    								str = scz.split("-")[0];// 取纸板编码如：2b8b2
    								klei = scz.split("-")[1];// 取坑类
    							} catch (Exception e1) {

    								MessageBox.showMessageDialog("提示", "已选择的物料编码"
    										+ scz + "不规范");
    								return;
    							}

    							if (str != null) {
    								for (int i = 0; i < str.length(); i++) {
    									// 保存与该原纸同样的表体VO，进行叠加
    									ZbndBVO ycbvo = new ZbndBVO();
    									// 判断表体唯一性的字段
    									int shu = 0;
    									// 原纸编码如2-1250
    									String yzkey = str.substring(i, i + 1)
    											+ "-" + poVO.getVdef3().toString();
    									//原址编码 替换
    									String sql =  "select bd_material.*  from bd_material where bd_material.code = '"+yzkey+"'  and nvl(bd_material.dr,0)=0 ";
    									MaterialVO vo = (MaterialVO)ce.executeQuery(sql, new BeanProcessor(MaterialVO.class));
    									if(vo.getDef4()!=null){
    										MaterialVO vos = (MaterialVO)HYPubBO_Client.queryByPrimaryKey(MaterialVO.class, vo.getDef4());
    										yzkey = vos.getCode();
    									}
    										;
    									
    									Integer flag = 0;
    									for (int n = 0; n < vector.size(); n++) {
    										// 判断相同的时候才赋值
    										if (vector.get(n).getBdef1()
    												.equals(yzkey)) {
    											shu = n;
    											ycbvo = vector.get(n);
    											// 置是否重复标志位
    											flag++;
    										}
    									}
    									if (flag != 0) {

    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");
    										// 计算原纸需求
    										UFDouble yzkz = new UFDouble(0);
    										UFDouble yzjz = new UFDouble(0);
    										UFDouble yzz = new UFDouble(0);
    										double yzx = 0;

    										if (bvos.length > 0) {
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);
    											// 获取原纸克重
    											yzkz = new UFDouble(
    													bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = Math
    													.ceil(((yzz.doubleValue()) / (yzjz
    															.doubleValue())));

    											ycbvo.setYznumber(ycbvo
    													.getYznumber().add(
    															new UFDouble(yzx)));
    											ycbvo.setNeednumber(ycbvo
    													.getNeednumber().add(yzz));

    											vector.set(shu, ycbvo);
    											flag = 0;
    										}
    									} else {
    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");

    										if (bvos.length > 0) {

    											// 计算原纸需求
    											UFDouble yzkz = new UFDouble(0);
    											UFDouble yzjz = new UFDouble(0);
    											UFDouble yzz = new UFDouble(0);
    											UFDouble yzx = new UFDouble(0);
    											MaterialPlanVO[] mpvo = (MaterialPlanVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialPlanVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "' and pk_org = '"
    																	+ poVO.getPk_org()
    																	+ "'");

    											// 获取原纸克重
    											yzkz = new UFDouble(
    													bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = new UFDouble(yzz.div(yzjz));

    											ZbndBVO bvo = new ZbndBVO();
    											// 设置原纸编码
    											bvo.setCode(bvos[0].getPrimaryKey());
    											// 设置原纸名字
    											bvo.setName(bvos[0].getName());
    											// 设置来源单据类型
    											bvo.setCsrctype("55B4");
    											// 设置提前期
    											bvo.setHeadtime(mpvo[0]
    													.getFixedahead().toString());
    											// 设置组织
    											bvo.setPk_org(poVO.getPk_org());
    											bvo.setPk_org_v(poVO.getPk_org_v());
    											// 设置原纸幅宽
    											bvo.setYzfk(new UFDouble(poVO
    													.getVdef3()));
    											bvo.setYzkz(yzkz);
    											// 设置需求日期为当前日期
    											bvo.setNeedtime(new UFDate());
    											// 获取原纸需求重量
    											bvo.setNeednumber(yzz);
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);

    											// 设置单位
    											MaterialConvertVO[] mcvo = (MaterialConvertVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialConvertVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "'");
    											if (mcvo.length == 0) {
    												MessageBox
    														.showMessageDialog(
    																"提示",
    																"物料编码为"
    																		+ bvos[0]
    																				.getCode()
    																		+ "的辅计量单位表体为空");
    												return;
    											}
    											bvo.setUnit(mcvo[0].getPk_measdoc());
    											bvo.setYznumber(new UFDouble(
    													Math.ceil(((yzz
    															.doubleValue()) / (yzjz
    															.doubleValue())))));
    											bvo.setBdef1(str
    													.substring(i, i + 1)
    													+ "-"
    													+ poVO.getVdef3()
    															.toString());
    											vector.add(bvo);
    										}
    									}

    								}

    							}
    						}

    						// 双层纸的情况，前面有一个‘-’符号。
    						else {
    							try {

    								String yxzd = scz.substring(1, scz.length());
    								str = yxzd.split("-")[0];// 取纸板编码如：2b8b2
    								klei = yxzd.split("-")[1];// 取坑类
    							} catch (Exception e2) {

    								MessageBox.showMessageDialog("提示", "已选择的物料编码"
    										+ scz + "不规范");
    								return;
    							}

    							if (str.length() > 0) {
    								for (int i = 0; i < str.length(); i++) {
    									// 保存与该原纸同样的表体VO，进行叠加
    									ZbndBVO ycbvo = new ZbndBVO();
    									int shu = 0;
    									String yzkey = str.substring(i, i + 1)
    											+ "-" + poVO.getVdef3().toString();
    									String sql =  "select bd_material.*  from bd_material where bd_material.code = '"+yzkey+"'  and nvl(bd_material.dr,0)=0 ";
    									MaterialVO vo = (MaterialVO)ce.executeQuery(sql, new BeanProcessor(MaterialVO.class));
    									if(vo.getDef4()!=null){
    										MaterialVO vos = (MaterialVO)HYPubBO_Client.queryByPrimaryKey(MaterialVO.class, vo.getDef4());
    										yzkey = vos.getCode();
    									}
    									Integer flag = 0;
    									for (int n = 0; n < vector.size(); n++) {
    										if (vector.get(n).getBdef1()
    												.equals(yzkey)) {
    											shu = n;
    											ycbvo = vector.get(n);
    											// 置是否重复标志位
    											flag++;
    										}
    									}
    									if (flag != 0) {
    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");
    										// 计算原纸需求
    										UFDouble yzkz = new UFDouble(0);
    										UFDouble yzjz = new UFDouble(0);
    										UFDouble yzz = new UFDouble(0);
    										double yzx = 0;

    										if (bvos.length > 0) {
    											// 获取原纸克重
    											yzkz = new UFDouble(bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = Math
    													.ceil(((yzz.doubleValue()) / (yzjz
    															.doubleValue())));
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);
    											ycbvo.setYznumber(ycbvo
    													.getYznumber().add(
    															new UFDouble(yzx)));
    											ycbvo.setNeednumber(ycbvo
    													.getNeednumber().add(yzz));
    											vector.set(shu, ycbvo);
    										}
    									} else {
    										// 获取原纸的单位平方米的克重和一件的重量
    										MaterialVO[] bvos = (MaterialVO[]) HYPubBO_Client
    												.queryByCondition(
    														MaterialVO.class,
    														"code='" + yzkey + "'");

    										if (bvos.length > 0) {
    											// 计算原纸需求
    											UFDouble yzkz = new UFDouble(0);
    											UFDouble yzjz = new UFDouble(0);
    											UFDouble yzz = new UFDouble(0);
    											UFDouble yzx = new UFDouble(0);
    											MaterialPlanVO[] mpvo = (MaterialPlanVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialPlanVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "' and pk_org = '"
    																	+ poVO.getPk_org()
    																	+ "'");

    											// 获取原纸克重
    											yzkz = new UFDouble(
    													bvos[0].getDef5());
    											yzz = new UFDouble(poVO.getVdef1())
    													.multiply(
    															new UFDouble(poVO
    																	.getVdef2()))
    													.multiply(
    															poVO.getNaccponum()
    																	.multiply(
    																			yzkz)
    																	.div(1000000));
    											// 获取原纸的件重
    											yzjz = new UFDouble(
    													bvos[0].getDef3());
    											yzx = new UFDouble(yzz.div(yzjz));

    											ZbndBVO bvo = new ZbndBVO();
    											// 设置原纸编码
    											bvo.setCode(bvos[0].getPrimaryKey());
    											// 设置原纸名字
    											bvo.setName(bvos[0].getName());
    											// 设置来源单据类型
    											bvo.setCsrctype("55B4");
    											// 设置提前期
    											bvo.setHeadtime(mpvo[0]
    													.getFixedahead().toString());
    											// 设置组织
    											bvo.setPk_org(poVO.getPk_org());
    											bvo.setPk_org_v(poVO.getPk_org_v());
    											// 设置原纸幅宽
    											bvo.setYzfk(new UFDouble(poVO
    													.getVdef3()));
    											bvo.setYzkz(yzkz);
    											// 设置需求日期为当前日期
    											bvo.setNeedtime(new UFDate());
    											// 获取原纸需求重量
    											bvo.setNeednumber(yzz);
    											// 设置单位
    											MaterialConvertVO[] mcvo = (MaterialConvertVO[]) HYPubBO_Client
    													.queryByCondition(
    															MaterialConvertVO.class,
    															"pk_material ='"
    																	+ bvos[0]
    																			.getPk_material()
    																	+ "'");
    											bvo.setUnit(mcvo[0].getPk_measdoc());
    											bvo.setYznumber(new UFDouble(
    													Math.ceil(((yzz
    															.doubleValue()) / (yzjz
    															.doubleValue())))));
    											bvo.setBdef1(str
    													.substring(i, i + 1)
    													+ "-"
    													+ poVO.getVdef3()
    															.toString());
    											UFDouble nonhandnum = new UFDouble();
    											UFDouble jianNum = new UFDouble();
    											UFDouble orderNum = new UFDouble();
    											UFDouble pickNum = new UFDouble();
    											try {
    												nonhandnum = getNonhandnum(
    														bvos[0],
    														poVO.getPk_org());
    												jianNum = getJianNum(bvos[0],
    														poVO.getPk_org());
    												orderNum = getOrderNum(bvos[0],
    														poVO.getPk_org());
    												pickNum = getPickNum(null, null);

    											} catch (BusinessException e1) {
    												// TODO 自动生成的 catch 块
    												e1.printStackTrace();
    											}
    											ycbvo.setYzxkcl(nonhandnum);
    											ycbvo.setYzkcjs(jianNum.toString());
    											ycbvo.setZtsl(orderNum);
    											ycbvo.setScyjyl(pickNum);
    											UFDouble add = yzz.sub(nonhandnum)
    													.add(jianNum).add(pickNum)
    													.add(pickNum);
    											ycbvo.setJxq(add);
    											vector.add(bvo);
    										}
    									}

    								}

    							}

    						}

    					}

    				} catch (UifException e1) {
    					// TODO 自动生成的 catch 块
    					e1.printStackTrace();
    				} catch (BusinessException e2) {
						// TODO 自动生成的 catch 块
						e2.printStackTrace();
					}
                }
			
			}
			MessageBox.showMessageDialog("提示", "选中了" + listvos.size() + "数据");

			for (int i = 0; vector != null && i < vector.size(); i++) {

				this.billform.getBillCardPanel().addLine();
				this.billform.getBillCardPanel().getBillModel()
						.setBodyRowVO(vector.get(i), i);
				// .execEditFormulas(i);
			}
			this.billform.getBillCardPanel().getBillModel()
					.loadLoadRelationItemValue();
			this.billform.updateUI();
			// getDataManager().refresh();

			ShowStatusBarMsgUtil.showStatusBarMsg("获取完成",
					this.model.getContext());

			this.close();
		}
	}

	/**
	 * 初始化
	 */
	private void initialize() {
		setTitle("纸板MRP计划订单获取");
		this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		this.setSize(840, 450);// 设置dialog窗体大小
		this.getContentPane().setLayout(new BorderLayout());// 设置布局
		this.getContentPane().add(getPnl_center(), BorderLayout.CENTER);// 加载面板
		this.getContentPane().add(getPnl_south(), BorderLayout.SOUTH);

		String qishidate = this.qizhitime.substring(0, 10) + " 00:00:00";
		String enddate = this.jiezhitime.substring(0, 10) + " 23:59:59";

		StringBuilder zd1 = new StringBuilder();
		zd1.append(" (vdef18 = '~'  or vdef18 = null ) and ");
		zd1.append(" fbillstatus <> 0 and ");// 状态为确认状态时才可以生成请购单
		zd1.append(" '").append(qishidate).append("' <= dplanstarttime and dplanstarttime <= '").append(enddate).append("' and  nvl(dr,0)=0 ");

		PoVO[] pvos = null;
		try {
			pvos = (PoVO[]) HYPubBO_Client.queryByCondition(PoVO.class,
					zd1.toString());
			// for (int i = 0; i < pvos.length; i++) {
			// //pnl_down.addLine();
			// getPnl_down().setBodyAutoAddLine(pvos[i]);
			//
			// }
			zd1.delete(0, zd1.length());
			// 表体塞值
			pnl_down.getBillModel().setBodyDataVO(pvos);
			// 执行表体编辑公式
			for (int i = 0; i < pvos.length; i++) {
				getPnl_down().execBodyFormula(i, "cmaterialvid");
			}

			getPnl_down().updateUI();
			// pnl_down.getBillModel().isEnabled();

		} catch (UifException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}

		// getPnl_down().addActionListener(this);

	}

	/**
	 * 确定按钮控件
	 * 
	 * @return
	 */
	public UIButton getBtn_ok() {
		if (null == btn_ok) {
			btn_ok = new UIButton("确定");
			btn_ok.addActionListener(this);// 加载listener
		}
		return btn_ok;
	}

	/**
	 * 中央布局面板
	 * 
	 * @return
	 */
	public UIPanel getPnl_center() {
		if (null == pnl_center) {
			pnl_center = new UIPanel();
			pnl_center.setLayout(new GridLayout(1, 1));// 设置布局：表哥布局row/col : 1/1
			pnl_center.add(getPnl_down());

			pnl_center.setVisible(true);// 设置可见
		}
		pnl_center.setSize(240, 500);
		return pnl_center;
	}

	/**
	 * 南方布局面板
	 * 
	 * @return
	 */
	public UIPanel getPnl_south() {
		if (null == pnl_south) {
			pnl_south = new UIPanel();
			pnl_south.setLayout(new FlowLayout());// 设置布局
			pnl_south.add(getBtn_ok());
			pnl_south.add(getBtn_cancel());
			pnl_south.setVisible(true);// 设置可见
		}

		return pnl_south;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	public BillCardPanel getPnl_down() {
		if (null == pnl_down) {
			pnl_down = new BillCardPanel();
			pnl_down.loadTemplet("1001ZZ1000000000I0Y3");// 通过表pub_billtemplet表主键加载单据模板
			pnl_down.setBodyMultiSelect(true);
		}
		return pnl_down;
	}

	/**
	 * 取消按钮控件
	 * 
	 * @return
	 */
	public UIButton getBtn_cancel() {
		if (null == btn_cancel) {
			btn_cancel = new UIButton("取消");
			btn_cancel.addActionListener(this);// 加载listener
		}
		return btn_cancel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(btn_ok)) {
			onBoOk(e);
		}
		if (e.getSource().equals(btn_cancel)) {
			onBoCancel(e);
		}
	}

	/**
	 * @author zbsilent 获取结存数量 即现存量
	 * @param vo
	 * @param pk_org
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getNonhandnum(MaterialVO vo, String pk_org)
			throws BusinessException {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(" select ic_onhandnum.nonhandnum ");
		strBuffer.append(" from ic_onhanddim ");
		strBuffer.append(" inner join ic_onhandnum on ic_onhanddim.pk_onhanddim = ic_onhandnum.pk_onhanddim ");
		strBuffer.append(" where ic_onhanddim.cmaterialoid = '" + vo.getPk_material() + "' ");
		strBuffer.append(" and ic_onhanddim.pk_org = '" + pk_org + "' ");
		strBuffer.append(" and nvl(ic_onhanddim.dr, 0) = 0 ");
		strBuffer.append(" and nvl(ic_onhandnum.dr, 0) = 0 ");
		Map<String, Object> res = ce.executeQuery(strBuffer.toString(),
				new MapProcessor()) == null ? null : (Map<String, Object>) ce
				.executeQuery(strBuffer.toString(), new MapProcessor());
		UFDouble nonhandnum = new UFDouble();
		if (res != null) {
			Object obj = res.get("nonhandnum") == null ? null : res
					.get("nonhandnum");
			if (obj != null)
				nonhandnum = new UFDouble(obj.toString());
		}
		// StringBuffer
		return nonhandnum;
	}

	/**
	 * @author zbsilent 获取库存批次数简述
	 * @param vo
	 * @param pk_org
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getJianNum(MaterialVO vo, String pk_org) {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(" select count(ic_onhanddim.pk_batchcode) as jianshu ");
		strBuffer.append(" from ic_onhanddim ");
		strBuffer.append(" inner join ic_onhandnum on ic_onhanddim.pk_onhanddim = ic_onhandnum.pk_onhanddim  ");
		strBuffer.append(" where ic_onhanddim.cmaterialoid = '"+ vo.getPk_material() + "' ");
		strBuffer.append(" and ic_onhanddim.pk_org = '" + pk_org + "' ");
		strBuffer.append(" and ic_onhandnum.nonhandnum > 0 ");
		strBuffer.append(" and nvl(ic_onhanddim.dr, 0) = 0 ");
		strBuffer.append(" and nvl(ic_onhandnum.dr, 0) = 0 ");
		Map<String, Object> res = null;
		UFDouble jianNum = new UFDouble();
		try {
			res = ce.executeQuery(strBuffer.toString(), new MapProcessor()) == null ? null : (Map<String, Object>) ce.executeQuery(strBuffer.toString(), new MapProcessor());
			if (res != null) {
				Object obj = res.get("jianshu") == null ? null : res.get("jianshu");
				if (obj != null)
					jianNum = new UFDouble(obj.toString());
			}
		} catch (BusinessException ex) {
			ex.printStackTrace();
		}

		// StringBuffer
		return jianNum;
	}

	/**
	 * 未关闭的未领用量
	 * 
	 * @param saleBillCode
	 * @param vo
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getPickNum(String saleBillCode, MaterialVO vo)
			throws BusinessException {
		return new UFDouble();

	}

	/**
	 * 获取在途数量
	 * 
	 * @param vo
	 * @param pk_org
	 * @return
	 * @throws BusinessException
	 */
	public UFDouble getOrderNum(MaterialVO vo, String pk_org)
			throws BusinessException {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(" select sum(((po_order_b.nnum  - po_order_b.naccumstorenum))) as ztnum, ");
		strBuffer.append(" po_order_b.vchangerate  ");
		strBuffer.append(" from po_order ");
		strBuffer.append(" inner join po_order_b  on po_order.pk_order = po_order_b.pk_order ");
		strBuffer.append(" where po_order_b.pk_material = '"+ vo.getPk_material() + "' ");
		strBuffer.append(" and po_order.pk_org ='" + pk_org + "' ");
		strBuffer.append(" and po_order.bfinalclose <> 'Y' ");
		strBuffer.append(" and po_order_b.bstockclose <> 'Y'  ");
		strBuffer.append(" group by   po_order_b.vchangerate  ");
		Map<String, Object> res = null;
		UFDouble orderNum = new UFDouble();
		try {
			res = ce.executeQuery(strBuffer.toString(), new MapProcessor()) == null ? null : (Map<String, Object>) ce.executeQuery(
							strBuffer.toString(), new MapProcessor());
			if (res != null) {
				Object ztnum = res.get("ztnum") == null ? null : res
						.get("ztnum");
				Object vchangerate = res.get("vchangerate") == null ? null
						: res.get("vchangerate");
				if (vchangerate != null && ztnum != null) {
					String[] split = vchangerate.toString().split("/");
					UFDouble hsl = new UFDouble(split[1]).div(new UFDouble(
							split[0]));
					orderNum = new UFDouble(ztnum.toString()).multiply(hsl);
				}

			}
		} catch (BusinessException ex) {
			ex.printStackTrace();
		}
		return orderNum;
	}
	
	 /**
	   * 
	   * @return
	   * @throws BusinessException
	   */
		public Boolean getTsMaterialId(String vfirstbid) throws BusinessException{
			//先查销售订单
			//SaleOrderHVO[] queryByCondition =(SaleOrderHVO[]) HYPubBO_Client.queryByCondition(SaleOrderHVO.class, "vbillcode = '"+vbillcode+"' and nvl(dr,0)=0");
			SaleOrderBVO[] queryByCondition =(SaleOrderBVO[]) HYPubBO_Client.queryByCondition(SaleOrderBVO.class, "CSALEORDERBID = '"+vfirstbid+"' and nvl(dr,0)=0");
			
			if(queryByCondition==null || queryByCondition.length<=0)
				return false;
			//字段标志
			String vbdef41 = queryByCondition[0].getVbdef41()==null?"":queryByCondition[0].getVbdef41();
			//比对是否有特殊 
			if(vbdef41.equals("Y")){
				return true;
			}else{
				return false;
			}
			 
		}
}
